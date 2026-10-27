# bayrol-poolmanager

A Java REST bridge for the BAYROL PoolManager web portal. It discovers the pools
available to your portal account, reads pH, chlorine and water temperature, and
exposes the actual light and filter-pump indicators as JSON and Prometheus
metrics. Pool lights can also be switched explicitly on or off.

## Build and run

Build with Maven and JDK 11 or later:

```sh
mvn package
java -jar bayrol-assembly/target/bayrolBridge.jar \
  -u=PORTAL_USERNAME -p=PORTAL_PASSWORD
```

The build runs unit tests for light and filter-pump status parsing, REST error
handling, separate measurement/output requests, and Prometheus export. Tests do
not connect to the portal or switch devices.

| Option | Purpose | Default |
| --- | --- | --- |
| `-u`, `--user` | BAYROL portal username; required | — |
| `-p`, `--pass` | BAYROL portal password; required | — |
| `-h`, `--httpPort` | HTTP listening port | `32176` |
| `-l`, `--logfile` | Log file location | `bayrol.log` |
| `-ll`, `--logLevel` | `FATAL`, `ERROR`, `WARN`, `INFO`, `DEBUG`, `TRACE`, `ALL` | `INFO` |
| `-s`, `--scheduler` | Parsed interval in milliseconds; currently not applied by the scheduler | `120000` |

Background measurement polling currently runs every **120 seconds**, fixed in
`BayrolBridge`. Changing `-s` does not change that interval. On portal failures,
the connector attempts to reconnect. Cached data can remain older during an
outage; a successful HTTP response from a cached endpoint does not imply a fresh
portal reading.

## REST API

Replace `POOLID` with an ID returned by `/getPlantIds`. All routes below use GET.

| Route | Result |
| --- | --- |
| `/Bayrol/getPlantIds` | IDs of pools discovered for the portal account |
| `/Bayrol/currentState/POOLID` | Last successful measurement snapshot as JSON |
| `/Bayrol/currentLiveState/POOLID` | Query the portal now and update the cached snapshot |
| `/Bayrol/currentState/POOLID/prometheus` | Cached snapshot in Prometheus text format |
| `/Bayrol/currentLiveState/POOLID/prometheus` | Fresh portal query in Prometheus text format |
| `/Bayrol/light/POOLID` | Live light indicator: `1` = on, `0` = off |
| `/Bayrol/filterPump/POOLID` | Live filter-pump indicator: `1` = running, `0` = off |
| `/Bayrol/setLight/POOLID/1` | Send an explicit light ON command |
| `/Bayrol/setLight/POOLID/0` | Send an explicit light OFF command |

The dedicated `/light` and `/filterPump` endpoints query the portal directly;
they do not return cached status. They return **HTTP 503** if the indicator is
missing, unknown, or cannot be retrieved. An unavailable cached measurement
snapshot also returns HTTP 503. A failed live measurement query returns an error
rather than silently falling back to cached data.

`/filterPump` is read-only. The bridge provides no pump-switching endpoint.
`/setLight` sends a command and returns `on` or `off`; this acknowledgement is
not confirmation of the actual light indicator or an independent physical
measurement. Light commands are idempotent ON/OFF commands, not toggle impulses.

Examples of read-only requests:

```sh
curl --fail http://localhost:32176/Bayrol/getPlantIds
curl --fail http://localhost:32176/Bayrol/currentState/POOLID
curl --fail http://localhost:32176/Bayrol/currentLiveState/POOLID
curl --fail http://localhost:32176/Bayrol/light/POOLID
curl --fail http://localhost:32176/Bayrol/filterPump/POOLID
```

## JSON snapshot and indicator meanings

```json
{
  "date": "2026-10-27T11:00:00.000+01:00",
  "ph": 7.25,
  "temp": 12.6,
  "cl": 0.08,
  "lightState": "1",
  "filterPumpState": "1"
}
```

`temp` is in °C; `cl` is in mg/l. `date` identifies the successful snapshot.
Measurement values and output indicators are read in **separate portal
requests**, because the portal omits output indicators when they are requested
together with measurement registers. They therefore belong to the same polling
cycle, but are not an atomic device sample.

JSON preserves the portal's raw indicator values:

| JSON field | Portal register | Raw `0` | Raw `1` | Other/missing value |
| --- | --- | --- | --- | --- |
| `lightState` | `55.17101.status` | On | Off | Unknown |
| `filterPumpState` | `55.17106.status` | Running | Off | Unknown |

These raw values are **inverted** relative to the normalized REST and Prometheus
values: raw `0` means on, whereas normalized `1` means on. Missing indicators are
represented by an empty string; unrecognized values must also be treated as
unknown.

The light operating-mode register `60.5433.value` is separate from the actual
indicator. Existing light commands write mode `1` for ON and `4` for OFF. The
filter pump's automatic/manual operating mode likewise does not establish
whether it is running. Status readings reflect BAYROL's portal indicators;
they are not independent sensors for physical light or water flow.

## Prometheus

Both cached and live measurement endpoints support `/prometheus`:

```sh
curl --fail http://localhost:32176/Bayrol/currentState/POOLID/prometheus
curl --fail http://localhost:32176/Bayrol/currentLiveState/POOLID/prometheus
```

Example for a snapshot with the light and filter pump off:

```text
BAYROL_POOL_TEMP{pool_id="POOLID"} 12.6
BAYROL_POOL_CL{pool_id="POOLID"} 0.08
BAYROL_POOL_PH{pool_id="POOLID"} 7.25
BAYROL_POOL_LIGHT{pool_id="POOLID"} 0
BAYROL_POOL_LIGHT_KNOWN{pool_id="POOLID"} 1
BAYROL_POOL_FILTER_PUMP{pool_id="POOLID"} 0
BAYROL_POOL_FILTER_PUMP_KNOWN{pool_id="POOLID"} 1
BAYROL_POOL_SAMPLE_TIMESTAMP_SECONDS{pool_id="POOLID"} 1793095200.0
```

| Metric suffix | Meaning |
| --- | --- |
| `TEMP` | Water temperature in °C |
| `CL` | Chlorine in mg/l |
| `PH` | pH value |
| `LIGHT` | `1` = on, `0` = off, `NaN` = unknown |
| `LIGHT_KNOWN` | `1` if the snapshot contains a recognized light indicator; otherwise `0` |
| `FILTER_PUMP` | `1` = running, `0` = off, `NaN` = unknown or stale |
| `FILTER_PUMP_KNOWN` | `1` only for a recognized pump indicator in a fresh snapshot; otherwise `0` |
| `SAMPLE_TIMESTAMP_SECONDS` | Snapshot time as Unix seconds; `NaN` for an invalid/missing date |

Pump export becomes `NaN` / `KNOWN=0` when the snapshot is more than **300
seconds old**, has an invalid date, or is more than 30 seconds in the future.
This prevents a stale cached ON state from looking like current pump operation.
The freshness limit is fixed; it does not change with polling configuration.
The existing temperature, chlorine, pH and light metrics retain their cached
values, so use the timestamp to assess their age as well.

Example Prometheus scrape configuration; replace `POOLID` and the host:

```yaml
scrape_configs:
  - job_name: bayrol
    metrics_path: /Bayrol/currentState/POOLID/prometheus
    scrape_interval: 30s
    static_configs:
      - targets: ['BAYROL_BRIDGE_HOST:32176']
```

Scraping the cached endpoint every 30 seconds does not increase the portal's
background polling rate. Use the live endpoint if each scrape must perform a
new portal query.
