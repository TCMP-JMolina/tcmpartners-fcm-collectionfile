# FCM FILE COLLECTIONS MODULE
This project handles the integration of the entities related to the
custom query of datasets in fusion cash management:

* File Collections.
* ... [TBD]

## Run Locally (Spring boot)

> **Important:**
> Please modify (to local) the `application-<env>.yaml`  file in `src/main/resources`.

Clone the project
```shell
git clone ****PENDING URL****
```

Go to the project directory
```shell
cd fcm-debts
```

And start the spring boot application
```shell
./mvnw spring-boot:run
```

## Run Locally (Azure) 
To start the project locally with azure functions
```shell
./mvnw -U clean package -DskipTests=true
```

## Deploy to Container registry
You can deploy the project with the following command (you must be logged in to the azure contariner registry)
```shell
./docker login <acrName>.azurecr.io
./docker build -t <imageNameENV> . 
./docker tag <imageNameENV> <acrName>.azurecr.io/<imageNameENV>:<tag>
./docker push <acrName>.azurecr.io/<imageNameENV>:<tag>
##########
./docker login acrpchdevwebapps01.azurecr.io 
./docker build -t asw-pch-fcmdev-sweepinterface-01 .
./docker tag asw-pch-fcmdev-sweepinterface-01 acrpchdevwebapps01.azurecr.io/asw-pch-fcmdev-sweepinterface-01:1.0
./docker push acrpchdevwebapps01.azurecr.io/asw-pch-fcmdev-sweepinterface-01:1.0 
./docker build -t asw-pch-fcmdev-sweepinterface-01 .; docker tag asw-pch-fcmdev-sweepinterface-01 acrpchdevwebapps01.azurecr.io/asw-pch-fcmdev-sweepinterface-01:1.0; docker push acrpchdevwebapps01.azurecr.io/asw-pch-fcmdev-sweepinterface-01:1.0
##########
./docker login acrpchqawebapps01.azurecr.io 
./docker build -t asw-pch-fcmqa-sweepinterface-01 .; docker tag asw-pch-fcmqa-sweepinterface-01 acrpchqawebapps01.azurecr.io/asw-pch-fcmqa-sweepinterface-01:1.0; docker push acrpchqawebapps01.azurecr.io/asw-pch-fcmqa-sweepinterface-01:1.0
./docker build -t asw-pch-fcmqa-paydet-01 .; docker tag asw-pch-fcmqa-paydet-01 acrpchqawebapps01.azurecr.io/asw-pch-fcmqa-paydet-01:24.0828; docker push acrpchqawebapps01.azurecr.io/asw-pch-fcmqa-paydet-01:24.0828
########## 
```

## Authors
TCM Partners - Frank Montagne 