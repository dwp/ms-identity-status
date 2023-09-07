#!/usr/bin/env sh

echo "Setting up mongo replication and seeding initial data..."

mongo mongo-primary:27017/example /tmp/replicate.js
echo "Replication done..."

sleep 20
mongo mongo-primary:27017/example /tmp/submission.js
echo "Seeding done..."
