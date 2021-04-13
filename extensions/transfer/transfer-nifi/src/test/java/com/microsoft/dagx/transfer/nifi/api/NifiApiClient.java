package com.microsoft.dagx.transfer.nifi.api;

import com.microsoft.dagx.spi.DagxException;
import com.microsoft.dagx.spi.types.TypeManager;
import okhttp3.*;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import static java.lang.String.format;

public class NifiApiClient {


    private final static String apiPath = "/nifi-api";
    private final URL url;
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final TypeManager typeManager;
    private final OkHttpClient httpClient;


    public NifiApiClient(String host, TypeManager typeManager, OkHttpClient httpClient) throws MalformedURLException {

        this.url = new URL(host);
        this.typeManager = typeManager;
        this.httpClient = httpClient;
    }

    public String uploadTemplate(String processGroup, File template) {

        try {
            var uploadUrl = new URL(url, apiPath + "/process-groups/" + processGroup + "/templates/upload");
            RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("template", "TwoClouds.xml", RequestBody.create(template, MediaType.parse("application/octet-stream")))
                    .build();
            Request request = new Request.Builder()
                    .url(uploadUrl)
                    .post(body)
                    .build();
            var response = makeCall(request);

            if (response.contains("<id>")) {
                var start = response.indexOf("<id>") + 4;
                var end = response.indexOf("</id>");
                return response.substring(start, end);
            } else {
                throw new DagxException("No <id> found in response!");
            }
        } catch (MalformedURLException e) {
            throw new DagxException(e);
        }
    }

    public void instantiateTemplate(String templateId) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("originX", 0.0);
        params.put("originY", 0.0);
        params.put("templateId", templateId);
        params.put("disconnectedNodeAcknowledged", true);

        try {
            var instantiateUrl = new URL(url, apiPath + "/process-groups/root/template-instance");
            var rq = new Request.Builder().url(instantiateUrl)
                    .post(RequestBody.create(typeManager.writeValueAsString(params), JSON))
                    .build();

            var response = makeCall(rq);


        } catch (MalformedURLException e) {
            throw new DagxException(e);
        }
    }

    public void startControllerService(String controllerServiceId, int version) {
        try {
            var u = new URL(url, apiPath + "/controller-services/" + controllerServiceId + "/run-status");


            var rqBody = new HashMap<String, Object>();
            rqBody.put("state", "ENABLED");
            rqBody.put("revision", new Revision(version));

            var rq = new Request.Builder().url(u)
                    .put(RequestBody.create(typeManager.writeValueAsString(rqBody), JSON))
                    .build();

            makeCall(rq);
        } catch (MalformedURLException e) {
            throw new DagxException(e);
        }
    }

    public List<ControllerService> getControllerServices(String processGroup) {

        try {

            var u = new URL(url, apiPath + "/flow/process-groups/" + processGroup + "/controller-services");
            var rq = new Request.Builder().url(u)
                    .get().build();
            var json = makeCall(rq);

            ControllerServiceResponse response = typeManager.readValue(json, ControllerServiceResponse.class);
            return response.controllerServices;

        } catch (MalformedURLException e) {
            throw new DagxException(e);
        }
    }

    public void startProcessGroup(String processGroup) {

        try {
            setState(processGroup, "RUNNING");
        } catch (MalformedURLException e) {
            throw new DagxException(e);
        }
    }

    public BulletinBoard getBulletinBoard() {
        try {

            var u = new URL(url, apiPath + "/flow/bulletin-board");
            var rq = new Request.Builder().url(u)
                    .get()
                    .build();
            var json = makeCall(rq);
            return typeManager.readValue(json, GetBulletinResponse.class).bulletinBoard;
        } catch (MalformedURLException e) {
            throw new DagxException(e);
        }
    }

    private void setState(String processGroup, String state) throws MalformedURLException {

        var dto = new StateDto(processGroup, state, true);
        var flowUrl = new URL(url, apiPath + "/flow/process-groups/" + processGroup);
        var rq = new Request.Builder().url(flowUrl)
                .put(RequestBody.create(typeManager.writeValueAsString(dto), JSON))
                .build();

        makeCall(rq);
    }

    private String makeCall(Request rq) {
        try (Response response = httpClient.newCall(rq).execute()) {
            int code = response.code();
            if (code < 200 || code >= 300) {
                throw new DagxException("Error in rest request: " + code + ", " + response.message());
            }
            return Objects.requireNonNull(response.body()).string();

        } catch (IOException e) {
            throw new DagxException(e);
        }
    }

}
