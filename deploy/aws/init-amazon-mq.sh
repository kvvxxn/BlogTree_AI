#!/usr/bin/env bash

set -euo pipefail

: "${MQ_API_BASE_URL:?Set MQ_API_BASE_URL, for example https://b-xxxx.mq.ap-northeast-2.amazonaws.com}"
: "${MQ_USERNAME:?Set MQ_USERNAME}"
: "${MQ_PASSWORD:?Set MQ_PASSWORD}"

MQ_VHOST="${MQ_VHOST:-/}"
ENCODED_VHOST="${MQ_VHOST_URLENCODED:-%2F}"

curl_json() {
  local method="$1"
  local path="$2"
  local data="${3:-}"

  if [[ -n "$data" ]]; then
    curl --fail --silent --show-error \
      -u "${MQ_USERNAME}:${MQ_PASSWORD}" \
      -H "content-type: application/json" \
      -X "$method" \
      "${MQ_API_BASE_URL}${path}" \
      -d "$data"
  else
    curl --fail --silent --show-error \
      -u "${MQ_USERNAME}:${MQ_PASSWORD}" \
      -H "content-type: application/json" \
      -X "$method" \
      "${MQ_API_BASE_URL}${path}"
  fi
}

echo "Configuring permissions for vhost ${MQ_VHOST}"
curl_json PUT "/api/permissions/${ENCODED_VHOST}/${MQ_USERNAME}" '{"configure":".*","write":".*","read":".*"}'

echo "Declaring exchanges"
curl_json PUT "/api/exchanges/${ENCODED_VHOST}/summary.exchange" '{"type":"topic","durable":true}'
curl_json PUT "/api/exchanges/${ENCODED_VHOST}/recommend.exchange" '{"type":"topic","durable":true}'
curl_json PUT "/api/exchanges/${ENCODED_VHOST}/dlx.exchange" '{"type":"direct","durable":true}'

echo "Declaring queues"
curl_json PUT "/api/queues/${ENCODED_VHOST}/summary.request.queue" '{"durable":true,"arguments":{"x-dead-letter-exchange":"dlx.exchange","x-dead-letter-routing-key":"summary.request.dead"}}'
curl_json PUT "/api/queues/${ENCODED_VHOST}/summary.response.queue" '{"durable":true,"arguments":{"x-dead-letter-exchange":"dlx.exchange","x-dead-letter-routing-key":"summary.response.dead"}}'
curl_json PUT "/api/queues/${ENCODED_VHOST}/summary.request.dlq" '{"durable":true}'
curl_json PUT "/api/queues/${ENCODED_VHOST}/summary.response.dlq" '{"durable":true}'
curl_json PUT "/api/queues/${ENCODED_VHOST}/recommend.request.queue" '{"durable":true,"arguments":{"x-dead-letter-exchange":"dlx.exchange","x-dead-letter-routing-key":"recommend.request.dead"}}'
curl_json PUT "/api/queues/${ENCODED_VHOST}/recommend.response.queue" '{"durable":true,"arguments":{"x-dead-letter-exchange":"dlx.exchange","x-dead-letter-routing-key":"recommend.response.dead"}}'
curl_json PUT "/api/queues/${ENCODED_VHOST}/recommend.request.dlq" '{"durable":true}'
curl_json PUT "/api/queues/${ENCODED_VHOST}/recommend.response.dlq" '{"durable":true}'

echo "Declaring bindings"
curl_json POST "/api/bindings/${ENCODED_VHOST}/e/summary.exchange/q/summary.request.queue" '{"routing_key":"summary.request","arguments":{}}'
curl_json POST "/api/bindings/${ENCODED_VHOST}/e/summary.exchange/q/summary.response.queue" '{"routing_key":"summary.response","arguments":{}}'
curl_json POST "/api/bindings/${ENCODED_VHOST}/e/recommend.exchange/q/recommend.request.queue" '{"routing_key":"recommend.request","arguments":{}}'
curl_json POST "/api/bindings/${ENCODED_VHOST}/e/recommend.exchange/q/recommend.response.queue" '{"routing_key":"recommend.response","arguments":{}}'
curl_json POST "/api/bindings/${ENCODED_VHOST}/e/dlx.exchange/q/summary.request.dlq" '{"routing_key":"summary.request.dead","arguments":{}}'
curl_json POST "/api/bindings/${ENCODED_VHOST}/e/dlx.exchange/q/summary.response.dlq" '{"routing_key":"summary.response.dead","arguments":{}}'
curl_json POST "/api/bindings/${ENCODED_VHOST}/e/dlx.exchange/q/recommend.request.dlq" '{"routing_key":"recommend.request.dead","arguments":{}}'
curl_json POST "/api/bindings/${ENCODED_VHOST}/e/dlx.exchange/q/recommend.response.dlq" '{"routing_key":"recommend.response.dead","arguments":{}}'

echo
echo "Amazon MQ bootstrap completed."
