#!/usr/bin/env python3

import argparse
from http_api_demo import *
from tree_render import *

#------------------------- parse args begin ------------------------#
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
                    help='Exact method path to trace, ordered, eg: "className1::methodName1[,className2::methodName2]"',
                    required=True)
parser.add_argument('--addit-method',
                    help='Additional methods to trace, unordered, but not in the method path, eg: "className1::methodName1[,className2::methodName2]"')
# parser.add_argument('--condition', '-c', help='condition express', default='')
parser.add_argument('--min-cost', help='min cost in condition: #cost > min_cost', type=int, default=0)
# TODO filter by min cost of specify method
parser.add_argument('--max-depth', help='Max trace depth (default:20)', type=int, default=20)
parser.add_argument('--timeout', '-t', help='request timeout(ms) (default:30000)', type=int, default=30000)
parser.add_argument('--skip-jdk-method', help='skip jdk method trace, (default:True)', type=str2bool, default=True)
parser.add_argument('--switch-path', help='Auto switch trace method path by stats data [totalCost] (default:True)', type=str2bool, default=True)
parser.add_argument('--reset-on-start', help='reset classes once on start (default:True)', type=str2bool, default=True)
parser.add_argument('--reset-on-round', help='reset classes on every round (default:False)', type=str2bool, default=False)
parser.add_argument('--stop-match-times',
                    help='If primary call tree matching times was exceeded, assuming no new call tree can be found (default: 10)',
                    type=int, default=10)

args = parser.parse_args()
# print args
print(args)

#------------------------- parse args end ------------------------#

as_url = 'http://' + args.host + "/api"
min_cost = args.min_cost
if min_cost > 0:
    condition = '"#cost > %s"' % (min_cost)
else:
    condition = ''
trace_times = args.times
timeout = args.timeout
stop_match_times = args.stop_match_times
is_skip_jdk_method = args.skip_jdk_method
is_switch_path = args.switch_path
max_depth = args.max_depth

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

# primary call tree match times
call_tree_match_times = 0

#
# {
#   method_path_code: {
#       'method_path': method_path,
#       'count': 2
#   }
# }
method_path_stats = {}


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



# stat trace data
def stat_trace_tree(root, method_path, method_path_code):
    trace_tree = root['children'][0]
    key = hex(method_path_code)
    stat = method_path_stats.get(key)
    if not stat:
        stat = {
            'method_path': list(method_path),
            'method_path_code': method_path_code,
            'count': 0,
            'totalCost': 0,
            'maxCost': 0,
            'minCost': 0,
            'avgCost': 0,
        }
        method_path_stats[key] = stat
    stat['count'] += 1
    stat['totalCost'] += trace_tree['totalCost']
    stat['avgCost'] = stat['totalCost']/stat['count']
    if stat['maxCost'] < trace_tree['maxCost']:
        stat['maxCost'] = trace_tree['maxCost']
    if stat['minCost'] > trace_tree['minCost']:
        stat['minCost'] = trace_tree['minCost']

def reset_method_path_stats():
    # reset method path stats
    for stat in method_path_stats.values():
        stat['count'] = 0
        stat['totalCost'] = 0
        stat['avgCost'] = 0
        stat['maxCost'] = 0
        stat['minCost'] = 0

def get_method_path_stat(method_path_code):
    key = hex(method_path_code)
    return method_path_stats.get(key)

def get_candidate_call_tree():
    return_stat = None
    total_cost = 0
    for key,stat in method_path_stats.items():
        if stat['totalCost'] > total_cost:
            return_stat = stat
            total_cost = stat['totalCost']
    return return_stat


def reset_trace_method_path(method_path):
    global trace_method_path
    trace_method_path = []
    for m in method_path:
        add_trace_method(m['className'], m['methodName'])
    reset_method_path_stats()


def handle_trace_result(context, result):
    type = result['type']
    if type == 'trace':
        root = result['root']
        method_path = match_call_tree(root)
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

        # switch primary call tree
        if is_switch_path:
            candidate_stat = get_candidate_call_tree()
            current_stat = get_method_path_stat(trace_method_path_code)
            if not current_stat:
                current_stat = {
                    "count": 0,
                    "method_path": trace_method_path,
                    "method_path_code": trace_method_path_code
                }
            candidate_method_path_code = candidate_stat['method_path_code']
            if candidate_method_path_code != trace_method_path_code and candidate_stat['count'] >= current_stat['count'] + 3:
                print("switch primary call tree from [%x] to [%x]" % (trace_method_path_code, candidate_method_path_code))
                new_method_path = candidate_stat["method_path"]
                print_method_path(new_method_path, candidate_method_path_code)
                reset_trace_method_path(new_method_path)
                interrupt_job(session_id)
                # return false, interrupt pull results
                return False

        # check call tree match times
        if call_tree_match_times >= stop_match_times:
            interrupt_job(session_id)
            return False

    return True


def add_additional_method(class_name, method_name):
    m = {'className': class_name, 'methodName': method_name}
    if m not in additional_methods:
        additional_methods.append(m)


def add_trace_method(class_name, method_name):
    tm = {
        'className': class_name,
        'methodName': method_name,
    }
    global trace_method_path_code
    trace_method_path.append(tm)
    trace_method_path_code = get_method_path_hash(trace_method_path)

    # add java.lang.reflect.InvocationHandler for java.lang.reflect.Proxy instance
    m = {"className": 'java.lang.reflect.InvocationHandler', "methodName": 'invoke'}
    if m not in additional_methods:
        class_detail = get_class_detail(session_id, class_name)
        if class_detail and is_derived_from(class_detail, 'java.lang.reflect.Proxy'):
            add_additional_method('java.lang.reflect.InvocationHandler', 'invoke')

    # java.lang.reflect.Method:invoke
    # if class_name == 'java.lang.reflect.Method' and method_name == 'invoke':

    return tm


def print_trace_method_path():
    print("trace method path: ")
    for tm in trace_method_path:
        print("    %s:%s()" % (tm['className'], tm['methodName']))

    print("additional methods: ")
    for am in additional_methods:
        print("    %s:%s()" % (am['className'], am['methodName']))

# replace regex chars
def replace_regex_chars(str):
    #in quote
    return str.replace("$", "\\$")
    #no quote
    #return str.replace("$", "\\\\$")

def start_trace():
    # concat trace regex match pattern
    # filter duplicated item by set
    global is_skip_jdk_method
    class_names = []
    method_names = []
    # append trace_method_path
    split_class_method_names(trace_method_path, class_names, method_names)

    # append additional methods
    split_class_method_names(additional_methods, class_names, method_names)

    class_pattern = "|".join(class_names)
    method_pattern = "|".join(method_names)
    class_pattern = replace_regex_chars(class_pattern)
    method_pattern = replace_regex_chars(method_pattern)

    command = 'trace -E "{0}" "{1}" {2} -n {3}'.format(class_pattern, method_pattern, condition, trace_times)
    if not is_skip_jdk_method:
        command += " --skipJDKMethod false"

    print("")
    print("start new trace:")
    print_trace_method_path()
    # print("command: %s" % command)

    # async exec trace
    context = async_exec(session_id, command)
    print("job_id: %d" % context['job_id'])

    return context


def split_class_method_names(method_path_or_list, class_names, method_names):
    for tm in method_path_or_list:
        class_name = tm['className']
        method_name = tm['methodName']
        if class_name not in class_names:
            class_names.append(class_name)
        if method_name not in method_names:
            method_names.append(method_name)


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


def print_method_path(method_path, method_path_code=None):
    if not method_path_code:
        method_path_code = get_method_path_hash(method_path )
    print("slow method path [%x]: " % method_path_code)
    for m in method_path:
        print("    %s:%s()" % (m["className"], m["methodName"]))
    print_method_path_as_arg(method_path)


def get_match_size(method_path1, method_path2):
    for index in range(len(method_path1)):
        if method_path1[index] != method_path2[index]:
            return index
    return len(method_path1)

# compare trace method path
# return:
#   method_path: new call tree found
#   None: match none / exact match / partial matching
def match_call_tree(root):
    trace_tree = root['children'][0]
    # compare trace method path
    global call_tree_match_times
    method_path = create_method_path_from_tree(trace_tree)
    method_path_code = get_method_path_hash(method_path)
    match_size = get_match_size(trace_method_path, method_path)
    if match_size == len(trace_method_path):
        if match_size == len(method_path):
            # exact match
            call_tree_match_times+=1
            stat_trace_tree(root, method_path, method_path_code)
            print("Exact matching primary call tree [%x] times: %d" % (trace_method_path_code, call_tree_match_times))
            return None
        elif len(method_path) > match_size:
            # new call tree
            stat_trace_tree(root, method_path, method_path_code)
            return method_path
        else:
            # error, len(method_path) < match_size
            raise Exception("Matching call tree error")
    elif match_size > 0:
        # 本次结果与之前的不完全匹配，如果方法时间比之前的大，应该进行修正
        stat_trace_tree(root, method_path, method_path_code)
        # print partial match on first meet
        print("Partial matching call tree [%x]" % method_path_code)
        if method_path not in partial_matching_method_paths:
            partial_matching_method_paths.append(method_path)
            print_trace_tree(root)
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


def replace_shell_chars(str):
    return str.replace("$", "\\$")

def print_method_path_as_arg(method_path):
    full_names = []
    for m in method_path:
        fullname = "%s:%s" % (m['className'], m['methodName'])
        full_names.append(fullname)
    print("As argument: "+replace_shell_chars(",".join(full_names)))
    print("")

def print_all_method_paths():
    print_method_path(trace_method_path)
    for mp in partial_matching_method_paths:
        print_method_path(mp)


"""
Main 
"""
if __name__ == "__main__":
    # init session
    (session_id, consumer_id) = init_session(as_url)
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

        # trace loop
        last_trace_method_path_code = 0
        while last_trace_method_path_code != trace_method_path_code:
            # async trace
            context = start_trace()
            reset_method_path_stats()
            last_trace_method_path_code = trace_method_path_code

            # pull results
            pull_results(context, consumer_id, ['trace'], handle_trace_result)

            # check call tree match times
            if call_tree_match_times >= stop_match_times:
                interrupt_job(session_id)
                print("The primary call tree matching times is exceeded, assuming no new call tree can be found.")
                break

            # check max trace depth
            if len(trace_method_path) > max_depth:
                print("Exceed max trace depth: %d" % len(trace_method_path))
                break

            # reset on round
            if args.reset_on_round:
                reset_classes()


        # print("")
        # print("")
        # print_all_method_paths()
        print("Job is finished.")
    except KeyboardInterrupt:
        print("")
        # print("")
        # print_all_method_paths()
        print("Job is canceled.")
    finally:
        interrupt_job(session_id)
