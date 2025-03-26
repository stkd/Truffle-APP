#!/bin/bash
# etcd-analyzer.sh

set -e

echo "===> [1] ETCD 資料庫基本資訊"
etcdctl endpoint status --write-out=table

echo
echo "===> [2] Top 10 Resource 類型 by Key 數量"
etcdctl get /registry/ --prefix --keys-only | cut -d'/' -f3 | sort | uniq -c | sort -nr | head -10

echo
echo "===> [3] Top 10 Namespace for CiliumEndpoints (Zombie CE?)"
etcdctl get /registry/cilium.io/ciliumendpoints --prefix --keys-only | cut -d'/' -f5 | sort | uniq -c | sort -nr | head -10

echo
echo "===> [4] Top 10 Key by Value Size (可能是大物件)"
etcdctl get /registry/ --prefix --write-out=json | \
jq -r '.kvs[] | {key: (.key | @base64d), size: (.value | @base64 | length)}' | \
sort -k2 -nr | head -10

echo
echo "===> [5] Top 10 Key by Modify Revision (頻繁變動的 key)"
etcdctl get /registry/ --prefix --write-out=json | \
jq -r '.kvs[] | {key: (.key | @base64d), modrev: .mod_revision}' | \
sort -k2 -nr | head -10