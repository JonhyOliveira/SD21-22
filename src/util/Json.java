package util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import tp1.impl.servers.common.replication.Version;

import java.io.IOException;

public class Json {

    private static Gson instance;

    public static Gson getInstance() {
        if (instance == null) {
            GsonBuilder builder = new GsonBuilder();
            registerTypeAdapters(builder);
            instance = builder.create();
        }

        return instance;
    }

    protected static void registerTypeAdapters(GsonBuilder builder) {
        builder.registerTypeAdapter(Version.class, new VersionTypeAdapter());
    }

}

class VersionTypeAdapter extends TypeAdapter<Version> {

    private enum NAMES {
        version,
        replicaID;
    }

    @Override
    public void write(JsonWriter jsonWriter, Version version) throws IOException {
        jsonWriter.beginObject()
                .name(NAMES.version.name())
                .value(version.v())
                .name(NAMES.replicaID.name())
                .value(version.replicaID())
        .endObject();
    }

    @Override
    public Version read(JsonReader jsonReader) throws IOException {

        Long version = -1L;
        String replicaID = "";

        jsonReader.beginObject();

        String fieldName = null;
        while (jsonReader.hasNext()) {
            JsonToken t = jsonReader.peek();

            if (t.equals(JsonToken.NAME))
                fieldName = jsonReader.nextName();

            if (NAMES.version.name().equals(fieldName))
                version = jsonReader.nextLong();

            if (NAMES.replicaID.name().equals(fieldName))
                replicaID = jsonReader.nextString();

        }

        jsonReader.endObject();

        return new Version(version, replicaID);
    }
}
