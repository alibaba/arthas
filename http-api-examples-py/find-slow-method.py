#!/usr/bin/env python3

import argparse
import requests

parser = argparse.ArgumentParser(description='Trace step by step, find slow method tree.')
parser.add_argument('--host', help='Arthas server host (default: 127.0.0.1:8563)', default="127.0.0.1:8563")
parser.add_argument('--times', '-n', help='Trace times of every round (default: 3)', type=int, default=3)
parser.add_argument('class_pattern', help='class name match pattern')
parser.add_argument('method_pattern', help='method name match pattern')
# parser.add_argument('--condition', '-c', help='condition express', default='')
parser.add_argument('min_cost', help='min cost in condition: #cost > min_cost')
parser.add_argument('--timeout', '-t', help='request timeout(ms) (default:30000)', default=30000)
args = parser.parse_args()
# print(args)

url = 'http://' + args.host + "/api"
class_pattern = args.class_pattern
method_pattern = args.method_pattern
min_cost = args.min_cost
condition = '"#cost > %s"' % (min_cost)
trace_times = args.times
timeout = args.timeout

# trace_methods
# {
#  class_name: xxx,
#  method_name: yyy,
#  name: xxx.yyy
#  parent: aaa.bbb
#  level: 1,
# }
trace_methods = []

# call stack tree
# {
#    class_name: xxx
#    method_name: xxx
#    children: [{
#       class_name: class_y
#       method_name: method_y
#    }]
# }
# call_stack_tree = None


def init_session():
    resp = requests.post(url, json={
        "action": "init_session"
    })
    # print(resp.text)
    result = resp.json()
    if resp.status_code == 200 and result['state'] == 'SUCCEEDED':
        session_id = result['sessionId']
        consumer_id = result['consumerId']
        return (session_id, consumer_id)

    raise Exception('init http session failed: ' + resp.text)

def interrupt_job(session_id):
    resp = requests.post(url, json={
        "action": "interrupt_job",
        "sessionId": session_id
    })
    # print(resp.text)
    result = resp.json()
    if resp.status_code == 200 and result['state'] == 'SUCCEEDED':
        return
    else:
        raise Exception('init http session failed: ' + resp.text)


def async_exec(session_id, command):
    resp = requests.post(url, json={
        "action": "async_exec",
        "command": command,
        "sessionId": session_id
    })
    # print(resp.text)
    result = resp.json()
    state = result['state']
    if resp.status_code == 200 and state == 'SCHEDULED':
        return result['body']['jobId']
    else:
        raise Exception('async exec failed: ' + resp.text)


# pull results of job
def pull_results(session_id, consumer_id, job_id, handler):
    while True:
        resp = requests.post(url, json={
            "action": "pull_results",
            "sessionId": session_id,
            "consumerId": consumer_id
        })
        # print(resp.text)
        json_resp = resp.json()
        state = json_resp['state']
        if resp.status_code == 200 and state == 'SUCCEEDED':
            results = json_resp['body']['results']
            for result in results:
                if result.get('jobId') :
                    res_job_id = result['jobId'];
                    if res_job_id == job_id:
                        handler(result)
                        # receive status code of job, the job is terminated.
                        if result['type'] == 'status':
                            return
                    elif res_job_id > job_id:
                        #new job is executing
                        return
            # TODO handle no response, timeout, cancel job
        else:
            raise Exception('pull results failed: ' + resp.text)


def handle_trace_result(result):
    type = result['type']
    if type == 'trace':
        trace_tree = result['root']['children'][0]
        sub_method = find_next_call(trace_tree)
        if sub_method:
            interrupt_job(session_id)
            print_trace_tree(trace_tree)
            # call_stack_tree = trace_tree
            add_trace_method(sub_method)
            # start new trace
            job_id = start_trace()
            #pull results
            pull_results(session_id, consumer_id, job_id, handle_trace_result)


def add_trace_method(class_name, method_name, parent, level):
    trace_methods.append({
        'class_name': class_name,
        'method_name': method_name,
        'name': class_name+'.'+method_name,
        'parent': parent,
        'level': level
    })
    return

def start_trace():
    #concat trace regex match pattern
    class_pattern=None
    method_pattern=None
    for tm in trace_methods:
        class_name = tm['class_name']
        method_name = tm['method_name']
        if class_pattern:
            class_pattern += "|" + class_name
        else:
            class_pattern = class_name
        if method_pattern:
            method_pattern += "|" + method_name
        else:
            method_pattern = method_name

    command = "trace -E {0} {1} {2} -n {3}".format(class_pattern, method_pattern, condition, trace_times)
    job_id = async_exec(session_id, command)

    return job_id


def match_node(node, class_name, method_name):
    return node['class_name']==class_name and node['method_name']==method_name

def find_next_call(trace_tree):
    #TODO compare trace methods, ignore non-invoking node
    node = trace_tree
    for tm in trace_methods:
        level = tm['level']
        class_name = tm['class_name']
        method_name = tm['method_name']
        if level == 0:
            if not match_node(node, class_name, method_name):
                return None
        else :
            #find children
            children = node['children']
            found = False
            for child in children:
                if match_node(child, class_name, method_name):
                    found = True
                    node = child
                    break
            if not found:
                return None

    # find next trace node
    next_node = None
    children = node['children']
    for child in children:
        if next_node:
            if next_node['maxCost'] < child['maxCost']:
                next_node = child
        else:
            next_node = child

    return next_node


def print_trace_tree(tree):

    return

# init session
(session_id, consumer_id) = init_session()
print("session_id: {0}, consumer_id: {1}".format(session_id, consumer_id))

# add root trace method
add_trace_method(class_pattern, method_pattern, None, 0)

#async trace
job_id = start_trace()

#pull results
pull_results(session_id, consumer_id, job_id, handle_trace_result)

