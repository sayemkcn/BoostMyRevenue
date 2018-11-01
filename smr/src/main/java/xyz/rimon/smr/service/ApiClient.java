package xyz.rimon.smr.service;

import android.app.Activity;
import android.content.Context;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.OkHttpResponseAndParsedRequestListener;
import com.androidnetworking.interfaces.OkHttpResponseListener;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Response;
import xyz.rimon.ael.domains.Event;
import xyz.rimon.smr.SMR;
import xyz.rimon.smr.commons.Auth;
import xyz.rimon.smr.commons.Commons;
import xyz.rimon.smr.commons.Parser;
import xyz.rimon.smr.commons.Pref;
import xyz.rimon.smr.model.Promo;
import xyz.rimon.smr.model.User;
import xyz.rimon.smr.model.UserAuth;
import xyz.rimon.smr.model.UserRev;
import xyz.rimon.smr.utils.Logger;
import xyz.rimon.smr.utils.Validator;

/**
 * Created by SAyEM on 4/12/17.
 */

public class ApiClient {
    private ApiClient() {
    }

    public static void registerUser(final Activity context, final User user) {
        Validator.validateUser(user);

        AndroidNetworking.post(ApiEndpoints.REGISTER_URL)
                .addBodyParameter(ApiEndpoints.KEY_CLIENT_ID_CAMELCASE, Pref.getPreferenceString(context, Pref.KEY_CLIENT_ID))
                .addBodyParameter(ApiEndpoints.KEY_CLIENT_SECRET_CAMELCASE, Pref.getPreferenceString(context, Pref.KEY_CLIENT_SECRET))
                .addBodyParameter(ApiEndpoints.KEY_NAME, user.getName())
                .addBodyParameter(ApiEndpoints.KEY_EMAIL, Pref.getPreferenceString(context, Pref.KEY_EMAIL))
                .addBodyParameter(ApiEndpoints.KEY_USERNAME, Pref.getPreferenceString(context, Pref.KEY_USERNAME))
                .addBodyParameter(ApiEndpoints.KEY_PASSOWRD, Pref.getPreferenceString(context, Pref.KEY_CLIENT_ID))
                .addBodyParameter(ApiEndpoints.KEY_APP_NAME, Commons.getApplicationName(context))
                .addBodyParameter(ApiEndpoints.KEY_APP_PACKAGE_NAME, context.getPackageName())
                .setTag("test")
                .setPriority(Priority.MEDIUM)
                .build()
                .getAsOkHttpResponse(new OkHttpResponseListener() {
                    @Override
                    public void onResponse(Response response) {
                        ResponseHandler.onUserRegistration(context, user, response);
                    }

                    @Override
                    public void onError(ANError anError) {
                        ResponseHandler.onError(anError);
                    }
                });

    }

    public static void login(final Activity context) {
        if (!Pref.getPreference(context, Pref.KEY_INITIALIZED)) {
            reInitialize(context);
        }
        String clientIdSnapshot = Commons.getClientIdSnapshot(Pref.getPreferenceString(context, Pref.KEY_CLIENT_ID));

        AndroidNetworking.get(ApiEndpoints.OAUTH_TOKEN_URL)
                .addQueryParameter(ApiEndpoints.KEY_GRANT_TYPE, ApiEndpoints.VAL_PASSWORD)
                .addQueryParameter(ApiEndpoints.KEY_CLIENT_ID, Pref.getPreferenceString(context, Pref.KEY_CLIENT_ID))
                .addQueryParameter(ApiEndpoints.KEY_CLIENT_SECRET, Pref.getPreferenceString(context, Pref.KEY_CLIENT_SECRET))
                .addQueryParameter(ApiEndpoints.KEY_USERNAME, clientIdSnapshot + Pref.getPreferenceString(context, Pref.KEY_USERNAME))
                .addQueryParameter(ApiEndpoints.KEY_PASSOWRD, Pref.getPreferenceString(context, Pref.KEY_CLIENT_ID))
                .setTag("test")
                .setPriority(Priority.MEDIUM)
                .build()
                .getAsOkHttpResponseAndParsed(new TypeToken<UserAuth>() {
                }, new OkHttpResponseAndParsedRequestListener<UserAuth>() {
                    @Override
                    public void onResponse(Response okHttpResponse, UserAuth user) {
                        ResponseHandler.onUserLogin(context, okHttpResponse, user);
                    }

                    @Override
                    public void onError(ANError anError) {
                        // handle error
                        Logger.e(anError.getErrorCode() + "");
                    }
                });
    }

    public static void refreshToken(final Context context) {
        AndroidNetworking.get(ApiEndpoints.OAUTH_TOKEN_URL)
                .addQueryParameter(ApiEndpoints.KEY_GRANT_TYPE, ApiEndpoints.VAL_REFRESH_TOKEN)
                .addQueryParameter(ApiEndpoints.KEY_CLIENT_ID, Pref.getPreferenceString(context, Pref.KEY_CLIENT_ID))
                .addQueryParameter(ApiEndpoints.KEY_CLIENT_SECRET, Pref.getPreferenceString(context, Pref.KEY_CLIENT_SECRET))
                .addQueryParameter(ApiEndpoints.KEY_REFRESH_TOKEN, Pref.getPreferenceString(context, Pref.KEY_REFRESH_TOKEN))
                .setTag("test")
                .setPriority(Priority.MEDIUM)
                .build()
                .getAsOkHttpResponseAndParsed(new TypeToken<UserAuth>() {
                }, new OkHttpResponseAndParsedRequestListener<UserAuth>() {
                    @Override
                    public void onResponse(Response okHttpResponse, UserAuth user) {
                        ResponseHandler.onUserLogin(context, okHttpResponse, user);
                    }

                    @Override
                    public void onError(ANError anError) {
                        // handle error
                    }
                });
    }

    public static void postEvent(final Activity context, List<Event> eventList) {
        if (!Auth.isLoggedIn(context)) {
            login(context);
            return;
        }

        if (eventList == null) eventList = new ArrayList<>();
        JSONArray jsonArray;
        try {
            jsonArray = new JSONArray(Parser.getGson().toJson(eventList));
        } catch (JSONException e) {
            jsonArray = new JSONArray();
        }

        AndroidNetworking.post(ApiEndpoints.POST_EVENT_URL)
                .addHeaders("Content-Type", "application/json")
                .addQueryParameter(ApiEndpoints.KEY_ACCESS_TOKEN, Pref.getPreferenceString(context, Pref.KEY_ACCESS_TOKEN))
                .addJSONArrayBody(jsonArray)
                .setTag("test")
                .setPriority(Priority.MEDIUM)
                .build()
                .getAsOkHttpResponse(new OkHttpResponseListener() {
                    @Override
                    public void onResponse(Response response) {
                        ResponseHandler.onPostEvent(context, response);
                    }

                    @Override
                    public void onError(ANError anError) {
                        ResponseHandler.onError(context, anError);
                    }
                });
    }

    public static void loadUserRevenue(final Activity context, String month, String year) {
        AndroidNetworking.get(ApiEndpoints.GET_USER_REVENUE_URL)
                .addQueryParameter(ApiEndpoints.KEY_ACCESS_TOKEN, Pref.getPreferenceString(context, Pref.KEY_ACCESS_TOKEN))
                .addQueryParameter(ApiEndpoints.KEY_MONTH, month)
                .addQueryParameter(ApiEndpoints.KEY_YEAR, year)
                .setTag("test")
                .setPriority(Priority.MEDIUM)
                .build()
                .getAsOkHttpResponseAndParsed(new TypeToken<UserRev>() {
                }, new OkHttpResponseAndParsedRequestListener<UserRev>() {
                    @Override
                    public void onResponse(Response okHttpResponse, UserRev userRev) {
                        ResponseHandler.onUserRevenueLoaded(context, okHttpResponse, userRev);
                    }

                    @Override
                    public void onError(ANError anError) {
                        login(context);
                    }
                });
    }

    public static void sendPaymentRequest(final Context context, String paymentMethod, String accountNumber, String amount, String note) {
        AndroidNetworking.post(ApiEndpoints.POST_PAYMENT_REQUEST_URL)
                .addQueryParameter(ApiEndpoints.KEY_ACCESS_TOKEN, Pref.getPreferenceString(context, Pref.KEY_ACCESS_TOKEN))
                .addQueryParameter(ApiEndpoints.KEY_PAYMENT_METHOD, paymentMethod)
                .addQueryParameter(ApiEndpoints.KEY_ACCOUNT_NUMBER, accountNumber)
                .addQueryParameter(ApiEndpoints.KEY_REQUEST_AMOUNT, amount)
                .addQueryParameter(ApiEndpoints.KEY_NOTE, note)
                .setTag("test")
                .setPriority(Priority.MEDIUM)
                .build()
                .getAsOkHttpResponse(new OkHttpResponseListener() {
                    @Override
                    public void onResponse(Response response) {
                        Logger.i(response.request().url().toString());
                        ResponseHandler.onPostPaymentRequest(context, response);
                    }

                    @Override
                    public void onError(ANError anError) {
                        Logger.i(anError.getResponse().request().url().toString());
                        ResponseHandler.onError(anError);
                    }
                });
    }

    public static void getPromoInformations(final Context context) {
        AndroidNetworking.get(ApiEndpoints.GET_PROMO_URL)
                .setTag("test")
                .setPriority(Priority.MEDIUM)
                .build()
                .getAsOkHttpResponseAndParsed(new TypeToken<Promo>() {
                }, new OkHttpResponseAndParsedRequestListener<Promo>() {
                    @Override
                    public void onResponse(Response okHttpResponse, Promo promo) {
                        ResponseHandler.onPromoDownloaded(context, okHttpResponse, promo);
                    }

                    @Override
                    public void onError(ANError anError) {
                        // handle error
                        Logger.e(anError.getErrorCode() + "");
                    }
                });
    }


    private static void reInitialize(Activity context) {
        SMR.setUser(context,
                Pref.getPreferenceString(context, Pref.KEY_NAME),
                Pref.getPreferenceString(context, Pref.KEY_EMAIL));

    }

    public static void increasePromoImpression(Promo promo) {
        AndroidNetworking.get(ApiEndpoints.buildPromotionCounterUrl(promo, false))
                .setTag("test")
                .setPriority(Priority.MEDIUM)
                .build()
                .getAsOkHttpResponseAndParsed(new TypeToken<Promo>() {
                }, new OkHttpResponseAndParsedRequestListener<Promo>() {
                    @Override
                    public void onResponse(Response okHttpResponse, Promo promo) {
                        Logger.d("Promo: Increased!");
                    }

                    @Override
                    public void onError(ANError anError) {
                        // handle error
                        Logger.e(anError.getErrorCode() + "");
                    }
                });
    }

    public static void registerFirebaseUserToken(final Context context) {
        String fbUserToken = Pref.getPreferenceString(context, Pref.KEY_FIREBASE_TOKEN);
        if (fbUserToken == null || fbUserToken.isEmpty()) return;

        AndroidNetworking.post(ApiEndpoints.POST_FIREBASE_USER_TOKEN)
                .addQueryParameter(ApiEndpoints.KEY_ACCESS_TOKEN, Pref.getPreferenceString(context, Pref.KEY_ACCESS_TOKEN))
                .addQueryParameter(ApiEndpoints.KEY_TOKEN, fbUserToken)
                .setTag("test")
                .setPriority(Priority.MEDIUM)
                .build()
                .getAsOkHttpResponse(new OkHttpResponseListener() {
                    @Override
                    public void onResponse(Response response) {
                        Pref.savePreference(context, Pref.KEY_FIREBASE_TOKEN, "");
                        Logger.i(response.request().url().toString());
                    }

                    @Override
                    public void onError(ANError anError) {
                        Logger.e(anError.getResponse().request().url().toString());
                        ResponseHandler.onError(anError);
                    }
                });
    }
}
