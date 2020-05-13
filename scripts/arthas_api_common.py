
import requests

as_url = 'http://127.0.0.1:8563/api'

# Init session
def init_session(url=as_url):
    global as_url
    as_url = url
    print("as_url: %s" % as_url)
    resp = requests.post(as_url, json={
        "action": "init_session"
    })
    # print(resp.text)
    result = resp.json()
    if resp.status_code == 200 and result['state'] == 'SUCCEEDED':
        session_id = result['sessionId']
        consumer_id = result['consumerId']
        return (session_id, consumer_id)

    raise Exception('init http session failed: ' + resp.text)


# Cancel foreground job of session
def interrupt_job(session_id):
    resp = requests.post(as_url, json={
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
    print("exec command: " + command)
    resp = requests.post(as_url, json={
        "action": "exec",
        "command": command,
        "sessionId": session_id
    })
    # print(resp.text)
    result = resp.json()
    state = result['state']
    if resp.status_code == 200 and state == 'SUCCEEDED':
        job_id = result['body']['jobId']
        results = result['body']['results']
        return {
            'session_id': session_id,
            "command": command,
            'job_id': job_id,
            'results': results
        }
    else:
        raise Exception('exec command failed: ' + resp.text)


# Execute command async
def async_exec(session_id, command):
    print("async exec command: " + command)
    resp = requests.post(as_url, json={
        "action": "async_exec",
        "command": command,
        "sessionId": session_id
    })
    # print(resp.text)
    result = resp.json()
    state = result['state']
    if resp.status_code == 200 and state == 'SCHEDULED':
        job_id = result['body']['jobId']
        return {
            'session_id': session_id,
            "command": command,
            'job_id': job_id
        }
    else:
        raise Exception('async exec command failed: ' + resp.text)


# filter results by types
def filter_results(context, data_types):
    new_results = []
    for result in context.get('results'):
        if result['type'] in data_types:
            new_results.append(result)
    return new_results


def get_class_detail(session_id, class_name):
    command = "sc -d " + class_name
    context = exec_command(session_id, command)
    for result in filter_results(context, ['class']):
        if result['classInfo']['name'] == class_name:
            return result['classInfo']

def is_derived_from(class_detail, super_class):
    super_classes = class_detail['superClass']
    if not super_classes:
        return False
    for sc in super_classes:
        if sc == super_class:
            return True
    return False


# -------------------- command callback handler begin -------------------------#

def default_command_data(context, result):
    pass


def default_command_success(context, status, message):
    pass


def default_command_error(context, status, message):
    msg = "execute command error, status: %d, error: %s " % (status, message)
    print(msg)
    # raise Exception(msg)


# handle results by callback handler
def handle_results(context, data_types, data_handler, success_handler=default_command_success,
                   error_handler=default_command_error):
    results = context['results'];
    for result in results:
        if result['type'] in data_types:
            data_handler(context, result)
        elif result['type'] == 'status':
            if result['statusCode'] == 0:
                success_handler(context, result['statusCode'], result.get('message'))
            else:
                error_handler(context, result['statusCode'], result.get('message'))
            break


# pull results of job
def pull_results(context, consumer_id, data_types, data_handler, success_handler=default_command_success,
                 error_handler=default_command_error):
    context['consumer_id'] = consumer_id
    session_id = context['session_id']
    job_id = context['job_id']
    while True:
        resp = requests.post(as_url, json={
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
                        # receive status code of job, the job is terminated.
                        if result['type'] == 'status':
                            if result['statusCode'] == 0:
                                success_handler(context, result['statusCode'], result.get('message'))
                            else:
                                error_handler(context, result['statusCode'], result.get('message'))
                            return

                        # handle data
                        if result['type'] in data_types:
                            r = data_handler(context, result)
                            if r != None and r == False:
                                # return False to skip processing
                                return
                    elif res_job_id > job_id:
                        # new job is executing, stop pull results
                        break
            # TODO handle no response, timeout, cancel job
        else:
            raise Exception('pull results failed: ' + resp.text)


# -------------------- command callback handler end -------------------------#
