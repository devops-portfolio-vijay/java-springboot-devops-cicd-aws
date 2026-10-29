#!/usr/bin/env python3
"""Exercise a running app with synthetic data; clean up the created record."""
import json
import os
import time
import urllib.error
import urllib.request

BASE = os.environ.get('APP_URL', 'http://localhost:8080').rstrip('/')

def request(path, method='GET', data=None):
    body = None if data is None else json.dumps(data).encode()
    req = urllib.request.Request(BASE + path, data=body, method=method,
                                 headers={'Content-Type': 'application/json'})
    with urllib.request.urlopen(req, timeout=5) as response:
        raw = response.read()
        return response.status, json.loads(raw) if raw else None

for attempt in range(60):
    try:
        _, health = request('/actuator/health/readiness')
        assert health['status'] == 'UP'
        break
    except (OSError, AssertionError):
        if attempt == 59:
            raise
        time.sleep(2)

student_id = None
try:
    _, health = request('/actuator/health')
    assert health['status'] == 'UP'
    _, release = request('/actuator/info')
    assert release['app']['version']
    with urllib.request.urlopen(BASE + '/', timeout=5) as page:
        assert b'Student Management' in page.read()
    sample = {'name': 'Pipeline Test', 'email': 'pipeline@example.com', 'course': 'AWS DevOps'}
    status, student = request('/students', 'POST', sample)
    assert status == 201
    student_id = student['id']
    path = f'/students/{student_id}'
    _, saved = request(path)
    assert saved['name'] == sample['name']
    _, students = request('/students')
    assert any(s['id'] == student_id for s in students)
    sample['course'] = 'Cloud Security'
    _, updated = request(path, 'PUT', sample)
    assert updated['course'] == 'Cloud Security'
    try:
        request('/students', 'POST', {'name': '', 'email': 'invalid', 'course': ''})
        raise AssertionError('Invalid input was accepted')
    except urllib.error.HTTPError as error:
        assert error.code == 400
    status, _ = request(path, 'DELETE')
    assert status == 204
    student_id = None
    try:
        request(path)
        raise AssertionError('Deleted student still exists')
    except urllib.error.HTTPError as error:
        assert error.code == 404
    print('PASS: web page, health, readiness, release info, CRUD, validation, and 404')
finally:
    if student_id is not None:
        request(f'/students/{student_id}', 'DELETE')
