package com.microsoft.dagx.client.command.http;

import com.microsoft.dagx.client.command.CommandResult;
import com.microsoft.dagx.client.command.ExecutionContext;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static com.microsoft.dagx.client.common.Output.error;


/**
 *
 */
public class HttpOperations {

    public static CommandResult executePost(String path, Object payload, ExecutionContext context) {
        Request request = new Request.Builder().url(context.getEndpointUrl() + path).post(context.write(payload)).build();
        return executeOperation(request, context);
    }

    public static CommandResult executeDelete(String path, Object payload, ExecutionContext context) {
        Request request = new Request.Builder().url(context.getEndpointUrl() + path).delete(context.write(payload)).build();
        return executeOperation(request, context);
    }

    public static CommandResult executeGet(String path, ExecutionContext context) {
        Request request = new Request.Builder().url(context.getEndpointUrl() + path).get().build();
        return executeOperation(request, context);
    }

    @NotNull
    private static CommandResult executeOperation(Request request, ExecutionContext context) {
        OkHttpClient client = HttpFactory.create(context);
        try (Response response = client.newCall(request).execute()) {
            return handleResponse(response, context);
        } catch (IOException e) {
            error(e, context.getTerminal());
            return new CommandResult(true, "");
        }
    }

    @NotNull
    private static CommandResult handleResponse(Response response, ExecutionContext context) throws IOException {
        ResponseBody responseBody = response.body();
        String message;
        if (responseBody == null) {
            message = response.code() + "";
        } else {
            message = responseBody.string();
            if (message.length() == 0) {
                message = response.code() + "";
            }
        }
        int code = response.code();
        if (code != 200) {
            return new CommandResult(code >= 300, code + ":" + message);
        }
        if (message.length() < 10000) {
            context.getTerminal().writer().println("Response: " + message);
        }
        return new CommandResult(message);
    }

    private HttpOperations() {
    }
}
