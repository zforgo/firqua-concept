package io.github.zforgo.firqua.openapi;

public interface OpenApiConstants {

    String TAG_admin = "admin";
    String TAG_assets = "assets";
    String TAG_devices = "devices";

    String TYPE_assetCreateUnion = "AssetCreateUnion";
    String TYPE_assetUnion = "AssetUnion";
    String TYPE_deviceUnion = "DeviceUnion";
    String TYPE_deviceCreateUnion = "DeviceCreateUnion";

    String STATUS_ok = "200";
    String STATUS_created = "201";

    String RESP_ok = "ok";
    String RESP_badRequest = "bad_request";
    String RESP_notFound = "not_found";
    String RESP_conflict = "conflict";
    String RESP_serverError = "server_error";
}
