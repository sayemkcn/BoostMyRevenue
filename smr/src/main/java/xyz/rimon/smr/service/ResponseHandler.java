package xyz.rimon.smr.service;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.androidnetworking.error.ANError;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;

import okhttp3.Response;
import xyz.rimon.ael.logger.Ael;
import xyz.rimon.smr.commons.Auth;
import xyz.rimon.smr.commons.Pref;
import xyz.rimon.smr.events.LoginEvent;
import xyz.rimon.smr.events.PaymentRequestEvent;
import xyz.rimon.smr.events.PostEventsEvent;
import xyz.rimon.smr.events.RevenueLoadEvent;
import xyz.rimon.smr.model.Promo;
import xyz.rimon.smr.model.User;
import xyz.rimon.smr.model.UserAuth;
import xyz.rimon.smr.model.UserRev;
import xyz.rimon.smr.utils.Logger;

/**
 * Created by SAyEM on 4/12/17.
 */

public class ResponseHandler {

    public static void onUserRegistration(Activity context, User user, Response response) {
        if (response.code() == 200 || response.code() == 226) { // if user created of already exists
            Pref.savePreference(context, Pref.KEY_INITIALIZED, true);
            ApiClient.login(context);
        } else {
            Pref.savePreference(context, Pref.KEY_INITIALIZED, false);
            onError(response);
        }
    }

    public static void onUserLogin(Context context, Response response, UserAuth auth) {
//        UserAuth auth = Parser.parseUserAuth(response);
        if (auth != null) {
            Auth.setLoggedIn(context, auth);
            ApiClient.registerFirebaseUserToken(context);
            EventBus.getDefault().post(new LoginEvent(auth));
        } else {
            // check if initialised, if not then initialized
//            if (!Pref.getPreference(context,Pref.KEY_INITIALIZED))
//                Auth.sse
            Pref.savePreference(context, Pref.KEY_LOGGED_IN, false);
            ApiClient.refreshToken(context);
        }
        Logger.i("AUTH " + auth.toString());
    }

    private static void onError(Response response) {
        Logger.e("RESPONSE_CODE:" + response.code() + "\nMESSAGE:" + response.message());
    }

    public static void onError(ANError anError) {
        Logger.e(anError.getErrorCode() + ":" + anError.getMessage());
    }

    public static void onError(Activity context, ANError anError) {
        if (anError.getErrorCode() == 417)
            Ael.clearEvents(context);
    }

    public static void onPostEvent(Activity context, Response response) {
        if (response.code() == 200) {
            Logger.i(String.valueOf(response.code()));
            Ael.clearEvents(context);
            EventBus.getDefault().post(new PostEventsEvent(true));
        } else if (response.code() == 401) {
            ApiClient.refreshToken(context);
            Logger.e(response.code() + ": Access_token:" + Pref.getPreferenceString(context, Pref.KEY_ACCESS_TOKEN));
        } else if (response.code() == 417) {
            Ael.clearEvents(context);
        }
    }

    public static void onPostPaymentRequest(Context context, Response response) {
        if (response.code() == 200)
            EventBus.getDefault().post(new PaymentRequestEvent(true));
        else if (response.code() == 406) {
            String message = "";
            try {
                message = response.body().string();
            } catch (IOException e) {
                e.printStackTrace();
            }
            EventBus.getDefault().post(new PaymentRequestEvent(true, message));
        } else
            EventBus.getDefault().post(new PaymentRequestEvent(true, "Could not perform this operation. Please try again in a little while!"));
    }

    public static void onUserRevenueLoaded(Context context, Response response, UserRev userRev) {
        if (response.code() == 200) {
            EventBus.getDefault().post(new RevenueLoadEvent(userRev));
            return;
        }
        Log.e("LOAD_REVENUE_CODE", String.valueOf(response.code()));
    }

    public static void onPromoDownloaded(Context context, Response okHttpResponse, Promo promo) {
        if (promo != null) {
            ApiClient.increasePromoImpression(promo);
            NotificationService.showNotification(context, promo.getTitle(), promo.getDescription(), ApiEndpoints.buildPromotionCounterUrl(promo, true));
        }
    }

}
