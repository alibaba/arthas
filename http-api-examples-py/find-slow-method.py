#!/usr/bin/env python3

import argparse
import requests
from datetime import datetime

def str2bool(v):
    if isinstance(v, bool):
        return v
    if v.lower() in ('yes', 'true', 't', 'y', '1'):
        return True
    elif v.lower() in ('no', 'false', 'f', 'n', '0'):
        return False
    else:
        raise argparse.ArgumentTypeError('Boolean value expected.')

parser = argparse.ArgumentParser(description='Trace step by step, find slow method tree.')
parser.add_argument('--host', help='Arthas server host (default: 127.0.0.1:8563)', default="127.0.0.1:8563")
parser.add_argument('--times', '-n', help='Max trace times of every round (default: 100)', type=int, default=100)
parser.add_argument('--method-path', '-m',
                    help='Exact method path to trace, eg: "className1::methodName1[,className2::methodName2]"',
                    required=True)
parser.add_argument('--addit-method',
                    help='Additional methods to trace, but not in the method path, '
                         'eg: "className1::methodName1[,className2::methodName2]"')
# parser.add_argument('--condition', '-c', help='condition express', default='')
parser.add_argument('--min-cost', help='min cost in condition: #cost > min_cost', type=int, default=0)
# TODO filter by min cost of specify method
parser.add_argument('--timeout', '-t', help='request timeout(ms) (default:30000)', type=int, default=30000)
parser.add_argument('--skip-jdk-method', help='skip jdk method trace, (default:True)', type=str2bool, default=True)
parser.add_argument('--reset-on-start', help='reset classes once on start (default:True)', type=str2bool, default=True)
parser.add_argument('--reset-on-round', help='reset classes on every round (default:False)', type=str2bool, default=False)
parser.add_argument('--stop-match-times',
                    help='If primary call tree matching times was exceeded, assuming no new call tree can be found (default: 10)',
                    type=int, default=10)
args = parser.parse_args()
# print args
print(args)

url = 'http://' + args.host + "/api"
min_cost = args.min_cost
if min_cost > 0:
    condition = '"#cost > %s"' % (min_cost)
else:
    condition = ''
trace_times = args.times
timeout = args.timeout
stop_match_times = args.stop_match_times
is_skip_jdk_method = args.skip_jdk_method


# trace method path, 方法的顺序与调用树一致
# {
#  className: xxx,
#  methodName: yyy
# }
trace_method_path = []
trace_method_path_code = 0

# Partial matching method paths
# [[{},{}], [path]]
partial_matching_method_paths = []

# additional trace methods, not in trace tree
additional_methods = []

call_tree_match_times = 0

# call stack tree
# {
#    className: xxx
#    methodName: xxx
#    children: [{
#       className: class_y
#       methodName: method_y
#    }]
# }
# call_stack_tree = None


"""
Tree rendering
"""
STEP_FIRST_CHAR = "`---"
STEP_NORMAL_CHAR = "+---"
STEP_HAS_BOARD = "|   "
STEP_EMPTY_BOARD = "    "


def nano_to_millis(nanoSeconds):
    return nanoSeconds / 1000000.0

def render_node(node):
    str = ''
    if node.get('threadName'):
        # thread
        str += "ts=%s;thread_name=%s;id=%s;is_daemon=%s;priority=%d;TCCL=%s" % (
            datetime.fromtimestamp(node['timestamp']/1000).strftime("%Y-%m-%d %H:%M:%S"),
            node['threadName'],
            hex(node['threadId']),
            node['daemon'],
            node['priority'],
            node['classloader']
        )
        if node.get('traceId'):
            str += ";trace_id="+node['traceId']
        if node.get('rpcId'):
            str += ";rpc_id="+node['rpcId']
    else:
        # cost
        times = node['times']
        if times == 1:
            str += '[%.3fms] ' % nano_to_millis(node['cost'])
        else:
            str += '[min=%.3fms,max=%.3fms,total=%.3fms,count=%d] ' % (nano_to_millis(node['minCost']),
                                                                       nano_to_millis(node['maxCost']),
                                                                       nano_to_millis(node['totalCost']),
                                                                       times)
        # method
        str += "%s:%s()" % (node['className'], node['methodName'])
        if node['lineNumber'] > 0:
            str += " #%d" % node['lineNumber']
    # mark
    if node.get('mark'):
        str += ' ['+node['mark']+']'
    return str

def print_node(prefix, node, is_last):
    current_prefix = (prefix+STEP_FIRST_CHAR) if is_last else (prefix+STEP_NORMAL_CHAR)
    print("%s%s" % (current_prefix, render_node(node)))
    # children
    if node.get('children'):
        children = node['children']
        size = len(children)
        for index in range(size):
            current_prefix = (prefix + STEP_EMPTY_BOARD) if is_last else (prefix + STEP_HAS_BOARD)
            is_last_child = (index == size-1)
            print_node(current_prefix, children[index], is_last_child)


def print_trace_tree(root):
    print_node('', root, True)

#------------------------ Tree Rendering End ---------------------------#


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
    if resp.status_code == 200:  # and result['state'] == 'SUCCEEDED'
        return result
    else:
        raise Exception('init http session failed: ' + resp.text)


# Execute command sync
def exec_command(session_id, command):
    resp = requests.post(url, json={
        "action": "exec",
        "command": command,
        "sessionId": session_id
    })
    # print(resp.text)
    result = resp.json()
    state = result['state']
    if resp.status_code == 200 and state == 'SUCCEEDED':
        return result['body']['results']
    else:
        raise Exception('exec command failed: ' + resp.text)


# Execute command async
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
        raise Exception('async exec command failed: ' + resp.text)


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
                if result.get('jobId'):
                    res_job_id = result['jobId'];
                    if res_job_id == job_id:
                        handler(result)
                        # check call tree match times
                        if call_tree_match_times >= stop_match_times:
                            print("The primary call tree matching times is exceeded, assuming no new call tree can be found.")
                            return
                        # receive status code of job, the job is terminated.
                        if result['type'] == 'status':
                            if result.get('message'): print(result['message'])
                            return
                    elif res_job_id > job_id:
                        # new job is executing
                        return
            # TODO handle no response, timeout, cancel job
        else:
            raise Exception('pull results failed: ' + resp.text)


def handle_trace_result(result):
    type = result['type']
    if type == 'trace':
        root = result['root']
        trace_tree = root['children'][0]
        method_path = match_call_tree(trace_tree)
        if method_path:
            # cancel job for executing other command
            interrupt_job(session_id)
            # add new method to path
            index = len(trace_method_path)
            size = len(method_path)
            while index < size:
                tm = method_path[index]
                add_trace_method(tm['className'], tm['methodName'])
                index += 1

            # print
            method_path_code = get_method_path_hash(method_path)
            print("New primary call tree [%x]" % method_path_code)
            print_trace_tree(root)
            print_method_path(method_path, method_path_code)
            print("")


def get_class_detail(class_name):
    command = "sc -d " + class_name
    results = exec_command(session_id, command)
    for result in results:
        type = result['type']
        if type == 'class' and result['classInfo']['name'] == class_name:
            return result['classInfo']
    pass


def is_derived_from(class_detail, super_class):
    super_classes = class_detail['superClass']
    for sc in super_classes:
        if sc == super_class:
            return True
    return False


def add_additional_method(class_name, method_name):
    # class_name = replace_regex_chars(class_name)
    # method_name = replace_regex_chars(method_name)
    additional_methods.append({'className': class_name, 'methodName': method_name})


def add_trace_method(class_name, method_name):
    # class_name = replace_regex_chars(class_name)
    # method_name = replace_regex_chars(method_name)
    tm = {
        'className': class_name,
        'methodName': method_name,
    }
    global trace_method_path_code
    trace_method_path.append(tm)
    trace_method_path_code = get_method_path_hash(trace_method_path)

    # add java.lang.reflect.InvocationHandler for java.lang.reflect.Proxy instance
    if 'java.lang.reflect.InvocationHandler' not in additional_methods:
        class_detail = get_class_detail(class_name)
        if is_derived_from(class_detail, 'java.lang.reflect.Proxy'):
            add_additional_method('java.lang.reflect.InvocationHandler', 'invoke')
    return tm


def print_trace_method_path():
    print("trace method path: ")
    for tm in trace_method_path:
        print("    %s:%s()" % (tm['className'], tm['methodName']))

    print("additional methods: ")
    for am in additional_methods:
        print("    %s:%s()" % (am['className'], am['methodName']))
    pass

# replace regex chars
def replace_regex_chars(str):
    return str.replace("$", "\\\\$")
    #.replace(".", "\\.")

def start_trace():
    # concat trace regex match pattern
    # filter duplicated item by set
    global is_skip_jdk_method
    class_names = []
    method_names = []
    for tm in trace_method_path:
        class_name = tm['className']
        method_name = tm['methodName']
        if class_name not in class_names:
            class_names.append(class_name)
        if method_name not in method_names:
            method_names.append(method_name)

    # append additional methods
    for am in additional_methods:
        class_name = am['className']
        method_name = am['methodName']
        if class_name not in class_names:
            class_names.append(class_name)
        if method_name not in method_names:
            method_names.append(method_name)

    class_pattern = "|".join(class_names)
    method_pattern = "|".join(method_names)
    class_pattern = replace_regex_chars(class_pattern)
    method_pattern = replace_regex_chars(method_pattern)

    command = "trace -E {0} {1} {2} -n {3}".format(class_pattern, method_pattern, condition, trace_times)
    if not is_skip_jdk_method:
        command += " --skipJDKMethod false"

    print("")
    print_trace_method_path()
    print("command: %s" % command)

    # async exec trace
    job_id = async_exec(session_id, command)
    print("job_id: %d" % job_id)

    return job_id


def match_node(node, class_name, method_name):
    return node['className'] == class_name and node['methodName'] == method_name


# 遍历调用树，生成关键方法路径
def create_method_path_from_tree(root):
    method_path = []
    node = root
    while node:
        class_name = node['className']
        method_name = node['methodName']
        method_path.append({'className': class_name, 'methodName': method_name})
        node = replace_duplicated_node(node, class_name, method_name)
        node = get_max_cost_node(node)

    return method_path


def get_method_path_hash(method_path):
    return hash(str(method_path))


def print_method_path(method_path, method_path_code):
    print("slow method path [%x]: " % method_path_code)
    for m in method_path:
        print("    %s:%s()" % (m["className"], m["methodName"]))


def get_match_size(method_path1, method_path2):
    for index in range(len(method_path1)):
        if method_path1[index] != method_path2[index]:
            return index
    return len(method_path1)

# compare trace method path
# return:
#   method_path: new call tree found
#   None: match none / exact match / partial matching
def match_call_tree(trace_tree):
    # compare trace method path
    global call_tree_match_times
    method_path = create_method_path_from_tree(trace_tree)
    match_size = get_match_size(trace_method_path, method_path)
    if match_size == len(trace_method_path):
        if match_size == len(method_path):
            # exact match
            call_tree_match_times+=1
            print("Exact matching primary call tree [%x] times: %d" % (trace_method_path_code, call_tree_match_times))
            return None
        elif len(method_path) > match_size:
            # new call tree
            return method_path
        else:
            # error, len(method_path) < match_size
            raise Exception("Matching call tree error")
    elif match_size > 0:
        # TODO 本次结果与之前的不完全匹配，如果方法时间比之前的大，应该进行修正
        # print partial match on first meet
        method_path_code = get_method_path_hash(method_path)
        print("Partial matching call tree [%x]" % method_path_code)
        if method_path not in partial_matching_method_paths:
            partial_matching_method_paths.append(method_path)
            print_trace_tree(trace_tree)
            print_method_path(method_path, method_path_code)
            print("")
        return None
    else:
        # match none
        # TODO match interface and it's impl class
        return None


def get_max_cost_node(node):
    children = node.get('children')
    if not children:
        return None

    next_node = None
    for child in children:
        if next_node:
            if next_node['maxCost'] < child['maxCost']:
                next_node = child
        else:
            next_node = child
    return next_node


def replace_duplicated_node(node, class_name, method_name):
    # ignore non-invoking node (fix Arthas duplicate enhance problem: https://github.com/alibaba/arthas/issues/599 )
    children = node.get('children')
    if children and len(children) == 1:
        sub_node = children[0]
        if match_node(sub_node, class_name, method_name) and not sub_node.get('invoking'):
            node = sub_node
    return node

def reset_classes():
    exec_command(session_id, 'reset')


"""
Main 
"""
# init session
(session_id, consumer_id) = init_session()
print("session_id: {0}, consumer_id: {1}".format(session_id, consumer_id))

try:
    # parse method path
    methods = args.method_path.split(',')
    for m in methods:
        strs = m.split(':')
        class_name = strs[0].strip()
        method_name = strs[1].replace('()','').strip()
        # add trace method
        add_trace_method(class_name, method_name)

    # parse addit-method
    if args.addit_method:
        methods = args.addit_method.split(',')
        for m in methods:
            strs = m.split(':')
            class_name = strs[0].strip()
            method_name = strs[1].replace('()','').strip()
            # additional method
            add_additional_method(class_name, method_name)

    if args.reset_on_start:
        reset_classes()

    trace_method_size = 0
    while trace_method_size < len(trace_method_path):
        # async trace
        job_id = start_trace()
        trace_method_size = len(trace_method_path)

        # pull results
        pull_results(session_id, consumer_id, job_id, handle_trace_result)
        # reset on round
        if args.reset_on_round:
            reset_classes()


    # print("")
    # print("")
    # print_trace_method_path()
    print("Job is finished.")
except KeyboardInterrupt:
    print("")
    print("Job is canceled.")
finally:
    interrupt_job(session_id)
    reset_classes()
