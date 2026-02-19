#!/usr/bin/env sh
# Register latest .avsc schemas to Schema Registry.
# Usage (from project root):
#   SCHEMA_REGISTRY_URL=http://localhost:8081 ./kafka/register_schemas.sh
# With port-forward: kubectl port-forward pod/schema-registry-1-0 8081:8081

set -e
SCHEMA_REGISTRY_URL="${SCHEMA_REGISTRY_URL:-http://localhost:8081}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SCHEMAS_DIR="${SCRIPT_DIR}/schemas"

for f in "${SCHEMAS_DIR}"/*.avsc; do
  [ -f "$f" ] || continue
  id=$(basename "$f" .avsc)
  schema=$(jq -c . "$f" | sed 's|"|\\"|g')
  echo "Registering subject: ${id}"
  curl -sSf -X POST -H "Content-Type: application/vnd.schemaregistry.v1+json" \
    --data "{\"schema\": \"${schema}\"}" \
    "${SCHEMA_REGISTRY_URL}/subjects/${id}/versions"
  echo ""
done
echo "Done."
