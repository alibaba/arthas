
from datetime import datetime

#------------------------ Tree Rendering Begin ---------------------------#

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
