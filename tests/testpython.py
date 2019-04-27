#!/usr/bin/env python

import time
import json
import time
import random

# Modify this parameter for logging to more files
file_count = 10
# Modify this parameter for more records per file
record_count = 100
# Sample ERROR record
error_str = 'ERROR: This is error log'

def generate_log():
	data = {}
	r = random.random()
	at = time.strftime('%a %b %d %H:%M:%S %Z %Y');
	data['at'] = at
	if r < 0.01:
		return error_str
	elif r < 0.9:
		data['note'] = 'content note'
	return json.dumps(data)

def output_logging():
	for i in range(0,file_count):
			with open('input/file%i.log' %i, 'w') as fo:
				for j in range(0,record_count):
					fo.write(generate_log()+'\n')

while True: # infinite loop
	output_logging();
	time.sleep(5); # Sleep 5 seconds between dumping logs
