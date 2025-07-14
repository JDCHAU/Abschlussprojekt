# Slurm RESTAPI

## JWT Token

scontrol token username=\<name\>

Non-root users can query for information (GET)

Currently only the root user can submit tasks by default (POST), if you need a new user you can add it yourself in the dockerfile or enter the node to add.

---

## Query-Example

see https://slurm.schedmd.com/SC24/REST-API.pdf

## Submit-Example

```shell
$ curl -X POST http://localhost:6820/slurm/v0.0.42/job/submit   -H "Content-Type: application/json"   -H "X-SLURM-USER-TOKEN: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE3NTIwNjI5ODEsImlhdCI6MTc1MjA2MTE4MSwic3VuIjoicm9vdCJ9.9qzlth8x1CDuRbhRsU1qi4WZpve73SzQUTrriiS1LvA"   --data-binary '{"script":"#!/bin/bash\nsrun hostname","job":{"name":"rest_test","partition":"normal","time_limit":300, "current_working_directory":"/data", "environment": [
 "PATH=/bin/:/usr/bin/:/sbin/"
]}}'
{
  "job_id": 9,
  "step_id": "batch",
  "job_submit_user_msg": "",
  "meta": {
    "plugin": {
      "type": "openapi\/slurmctld",
      "name": "Slurm OpenAPI slurmctld",
      "data_parser": "data_parser\/v0.0.42",
      "accounting_storage": "accounting_storage\/slurmdbd"
    },
    "client": {
      "source": "[slurmrestd]:42010(fd:9)",
      "user": "root",
      "group": "root"
    },
    "command": [
    ],
    "slurm": {
      "version": {
        "major": "24",
        "micro": "5",
        "minor": "11"
      },
      "release": "24.11.5",
      "cluster": "cluster"
    }
  },
  "errors": [
  ],
  "warnings": [
  ]

```

Java:

```java
public String submitByRoot(String token, JSONObject params) throws IOException, InterruptedException {
    //    MediaType type = MediaType.get("application/json; charset=utf-8");
    // here must use null as placeholder for type, type is here not allowed
    // add type in header
        RequestBody requestBody = RequestBody.create( null, params.toString() );

        Request req = new Request.Builder().url(URL.concat("job/submit")).
                addHeader("X-SLURM-USER-TOKEN", token).
            // add type in header
                addHeader("Content-Type", "application/json").
                post(requestBody).
                build();
        try (Response response = httpClient.newCall(req).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code() + " - " + response.message()
                        + "\n" + response.body().string());
            }
            String respStr = response.body().string();
            return respStr;
        }

    }

// cwd and env are both madatory
    @Test
    public void submit() throws IOException, InterruptedException {
        JSONObject job = new JSONObject()
                .put("name", "rest_smoke")
                .put("partition", "normal")
                .put("time_limit", 300)
                .put("current_working_directory", "/data")
                .put("environment", new JSONArray().put("PATH=/bin/:/usr/bin/:/sbin/"));

        JSONObject payload = new JSONObject()
                .put("script", "#!/bin/bash\nsrun hostname")
                .put("job", job);

        String token = slurmRest.generateRootToken("compute1");
        System.out.println(slurmRest.submitByRoot(token, payload));
    }
```

