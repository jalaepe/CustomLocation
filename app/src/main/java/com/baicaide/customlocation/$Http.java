package com.baicaide.customlocation;

import android.os.*;

import androidx.annotation.RequiresApi;

import java.io.*;
import java.net.*;
import java.util.*;

public class $Http extends AsyncTask<$Http.Response,Void,String>
{
    public static enum RequesMethod
    {
        GET("GET"),POST("POST"),PUT("PUT"),DELETE("DELETE");
        private final String stringValue;
        RequesMethod(final String s) {
            stringValue = s;
        }
        public String toString() {
            return stringValue;
        }
    }

    private Response response;
    private HttpURLConnection httpConn;
    private String url,errorMsg;
    boolean error=false;
    private Map<String,String> fields=new HashMap<>();
    private Map<String,String> files=new HashMap<>();
    private List<String> fileArray=new ArrayList<>();
    private Map<String,String> headers=new HashMap<>();
    private RequesMethod requestMethod;
    private String boundary;
    private String charset="UTF-8";
    private OutputStream outputStream;
    private PrintWriter writer;

    public static interface Response
    {
        public void onResponse(String response);
        public void onError(String msg);
    }

    public $Http(RequesMethod requestMethod)
    {
        this.requestMethod = requestMethod;
    }

    public $Http url(String url)
    {
        this.url = url;
        return this;
    }

    public $Http field(String name, String val)
    {
        fields.put(name, val);
        return this;
    }

    public $Http file(String name, String path)
    {
        files.put(name, path);
        return this;
    }

    public $Http fileArray(String path)
    {
        fileArray.add(path);
        return this;
    }

    public $Http header(String name, String val)
    {
        headers.put(name, val);
        return this;
    }

    public $Http auth(String token)
    {
        headers.put("Authentication", "Bearer " + token);
        return this;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public $Http auth(String username, String password)
    {
        try
        {
            String encoded = Base64.getEncoder().encodeToString((username + ":" + password).getBytes("UTF-8"));
            headers.put("Authentication", "Basic " + encoded);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return this;
    }

    public $Http charset(String val)
    {
        this.charset = val;
        return this;
    }

    private void addFormField(String name, String value)
    {
        this.writer.append("--" + this.boundary).append("\r\n");
        this.writer.append("Content-Disposition: form-data; name=\"" + name + "\"").append
                ("\r\n");
        this.writer.append("Content-Type: text/plain; charset=" + this.charset).append(
                "\r\n");
        this.writer.append("\r\n");
        this.writer.append(value).append("\r\n");
        this.writer.flush();
    }

    private void addFilePart(String fieldName, File uploadFile)
    {
        try
        {
            String fileName = uploadFile.getName();
            this.writer.append("--" + this.boundary).append("\r\n");
            this.writer.append(
                    "Content-Disposition: form-data; name=\"" + fieldName +
                            "\"; filename=\"" + fileName + "\"").append
                    ("\r\n");
            this.writer.append(
                    "Content-Type: " +
                            URLConnection.guessContentTypeFromName(fileName)).append
                    ("\r\n");
            this.writer.append("Content-Transfer-Encoding: binary").append("\r\n");
            this.writer.append("\r\n");
            this.writer.flush();

            FileInputStream inputStream = new FileInputStream(uploadFile);
            byte[] buffer = new byte[4096];
            int bytesRead = -1;
            while ((bytesRead = inputStream.read(buffer)) != -1)
                this.outputStream.write(buffer, 0, bytesRead);

            this.outputStream.flush();
            inputStream.close();
            this.writer.append("\r\n");
            this.writer.flush();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void addHeaderField(String name, String value)
    {
        this.writer.append(name + ": " + value).append("\r\n");
        this.writer.flush();
    }

    @Override
    protected String doInBackground($Http.Response[] p1)
    {
        this.response = p1[0];
        StringBuffer result=new StringBuffer();
        this.boundary = "===" + System.currentTimeMillis() + "===";
        try
        {
            this.httpConn = ((HttpURLConnection)new URL(url).openConnection());
            this.httpConn.setUseCaches(false);

            this.httpConn.setRequestMethod(this.requestMethod.toString());

            this.httpConn.setDoOutput(true);
            this.httpConn.setDoInput(true);
            this.httpConn.setRequestProperty("Content-Type",
                    "multipart/form-data; boundary=" + this.boundary);
            this.outputStream = this.httpConn.getOutputStream();
            this.writer = new PrintWriter(new OutputStreamWriter(this.outputStream, charset), true);

//========

            Iterator itr = headers.entrySet().iterator();
            while (itr.hasNext())
            {
                Map.Entry entry = (Map.Entry)itr.next();
                addHeaderField(entry.getKey().toString(), entry.getValue().toString());
            }

            itr = fields.entrySet().iterator();
            while (itr.hasNext())
            {
                Map.Entry entry = (Map.Entry)itr.next();
                addFormField(entry.getKey().toString(), entry.getValue().toString());
            }

            if(fileArray.size()>0){
                for(String path:fileArray)
                {
                    addFilePart("files[]", new File(path));
                }
                this.writer.append("\r\n").flush();
                this.writer.append("--" + this.boundary + "--").append("\r\n");
                this.writer.close();
            }

            itr = files.entrySet().iterator();
            while (itr.hasNext())
            {
                Map.Entry entry = (Map.Entry)itr.next();
                addFilePart(entry.getKey().toString(), new File(entry.getValue().toString()));

//========

                this.writer.append("\r\n").flush();
                this.writer.append("--" + this.boundary + "--").append("\r\n");
                this.writer.close();
            }

            int status = this.httpConn.getResponseCode();
            if (status == 200)
            {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(this.httpConn.getInputStream()));
                String line = null;
                while ((line = reader.readLine()) != null)
                    result.append(line).append("\n");

                reader.close();
                this.httpConn.disconnect();
            }
            else
            {
                errorMsg="Server returned non-OK status: " + status;
                error=true;
                return null;
            }

//========

        }
        catch (Exception e)
        {
            e.printStackTrace();
            errorMsg=e.toString();
            error=true;
            return null;
        }
        return result.toString();
    }

    @Override
    protected void onPostExecute(String result)
    {
        super.onPostExecute(result);
        if (this.response != null)
        {
            if(error){
                response.onError(errorMsg);
            }else{
                response.onResponse(result);
            }
        }
    }
}
