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
