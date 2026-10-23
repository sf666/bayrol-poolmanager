# bayrol-poolmanager
Rest Service on top of Bayrol Poolmanager Web-Portal

Start service with login parameter like

```
-u=PORTAL-USERNAME -p=PORTAL_PASSWORD
```

Optional parameter are:


```
-h PORT                          # Listen Port. Default : 32176
-s SCAN_INTERVAL                 # time between Bayrol Web-Service calls in milli seconds . Default : 120000           # (2 Minutes)
-l LOGFILE_LOCATION              # Location of logfile. Default : bayrol.log
-ll LOG_LEVEL                    # Available level are : FATAL, ERROR, WARN, INFO, DEBUG, TRACE, ALL". Default : INFO
```

Add your poolID to the request to collect values.


```
curl localhost:32176/Bayrol/currentState/POOLID
```

To get live pool data call 


```
curl localhost:32176/Bayrol/currentLiveState/POOLID
```

Default return format is JSON.

Output is

```
{"date":"2023-06-01T09:08:54.729+02:00","ph":7.26,"temp":34.3,"cl":0.66}
```


There is a prometheus exporer available. Append `/prometheus` to the request url.

```
curl localhost:32176/Bayrol/currentState/POOLID/prometheus
curl localhost:32176/Bayrol/currentLiveState/POOLID/prometheus
```

Output is prefixed with `BAYROL_POOL_`:

```
BAYROL_POOL_TEMP{pool_id="POOL_ID"} 34.3
BAYROL_POOL_CL{pool_id="POOL_ID"} 0.6
BAYROL_POOL_PH{pool_id="POOL_ID"} 7.27
```

## Light state

The measurement request also reads `60.5433.value`. JSON `lightState` preserves
that complete raw mode: `1` means explicitly ON, `4` explicitly OFF. Other modes
or a missing value are unknown; they are not treated as OFF. `date` is the time
of the successful measurement snapshot; `/currentState` can contain cached data.

Prometheus adds:

```
BAYROL_POOL_LIGHT{pool_id="POOL_ID"} 0
BAYROL_POOL_LIGHT_KNOWN{pool_id="POOL_ID"} 1
```

`LIGHT` is `1` for ON, `0` for OFF, and `NaN` when unknown. `LIGHT_KNOWN` is then
`1`, `1`, or `0`, respectively. A failed whole measurement request does not refresh
the cached snapshot; consumers must also consider its age.

`GET /Bayrol/light/POOLID` reads live and returns `1` or `0`; unavailable or
unknown modes return HTTP 503. `GET /Bayrol/setLight/POOLID/1` and `/0` are explicit
ON/OFF commands, not toggle impulses. A command response alone is not proof of
the physical light state. Tests do not connect to the portal or actuate devices.
