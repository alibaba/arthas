#!/usr/bin/env python3

from arthas_api_common import *



"""
Main 
"""
if __name__ == "__main__":
    as_url = 'http://127.0.0.1:8563/api'
    session_id = ''
    consumer_id = ''

    # init session
    (session_id, consumer_id) = init_session(as_url)
    print("session_id: {0}, consumer_id: {1}".format(session_id, consumer_id))

    # session
    context = exec_command(session_id, 'session')
    handle_results(context, ['session'], lambda context, result: {
        print("session: %s" % str(result))
    })

    # sc
    context = exec_command(session_id, 'sc -d java.lang.String')
    handle_results(context, ['class'], lambda context, result: {
        print("Detail of String: %s" % str(result['classInfo']))
    })

    # sc
    detail = get_class_detail(session_id, "java.util.List")
    print('Detail of List: %s' % str(detail))

    # cat
    context = exec_command(session_id, 'cat /tmp/thread.txt  /tmp/abc  ')
    handle_results(context, ['cat'], lambda context, result: {
        print("file: %s" % str(result))
    }, error_handler=lambda context, status, message: {
        print('read file failed: %s, error: %s' % (status, message))
    })

    # watch
    context = async_exec(session_id, 'watch demo.MathGame primes* "{params}" -n 5 ')
    pull_results(context, consumer_id, ['watch'], lambda context, result: {
        print("watch: %s" % str(result))
    }, error_handler=lambda context, status, message: {
        print('watch failed: %s, error: %s' % (status, message))
    })


    # result callback handler
    def watch_result_handler(context, result):
        print("watch: %s" % str(result))
        cost = result['cost']

        if not context.get('count'):
            context['count'] = 1
        else:
            context['count'] += 1
        if context['count'] > 5:
            # cancel job
            interrupt_job(context['session_id'])
            # return False to skip processing
            return False


    # watch
    context = async_exec(session_id, 'watch *HotelMapper selectByCityId "{returnObj}" ')
    pull_results(context, consumer_id, ['watch'], watch_result_handler)
