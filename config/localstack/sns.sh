#!/bin/bash

awslocal sns create-topic --name pip_identity_event_nino
awslocal sns create-topic --name update_pipcs_idv_topic
