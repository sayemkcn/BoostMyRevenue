package xyz.rimon.smr.views;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import xyz.rimon.ael.commons.Commons;
import xyz.rimon.smr.R;
import xyz.rimon.smr.commons.Pref;
import xyz.rimon.smr.commons.UIUtils;
import xyz.rimon.smr.events.LoginEvent;
import xyz.rimon.smr.events.PaymentRequestEvent;
import xyz.rimon.smr.events.PostEventsEvent;
import xyz.rimon.smr.events.RevenueLoadEvent;
import xyz.rimon.smr.model.UserRev;
import xyz.rimon.smr.service.ApiClient;
import xyz.rimon.smr.utils.Validator;

/**
 * TODO: document your custom view class.
 */
public class MyRevenueView extends LinearLayout implements View.OnClickListener, AdapterView.OnItemSelectedListener {

    private TextView cmInterationPoints;
    private TextView cmIncome;
    private TextView lastPaymentAmount;
    private TextView pmIncome;

    private View paymentView;
    private TextView tvRequestPaymentToggle;
    private Spinner spnPaymentMethod;
    private String paymentMethod;
    private EditText etAccountNumber;
    private EditText etAmount;
    private Button btnSendRequest;

    public MyRevenueView(Context context) {
        super(context);
        this.initView();
    }

    public MyRevenueView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.initView();
    }

    private void initView() {
        LayoutInflater.from(getContext()).inflate(R.layout.my_revenue_view, this);
        EventBus.getDefault().register(this);

        this.cmInterationPoints = this.findViewById(R.id.cmInteractionPoints);
        this.cmIncome = this.findViewById(R.id.cmIncome);
        this.lastPaymentAmount = this.findViewById(R.id.lastPaymentAmount);
        this.pmIncome = this.findViewById(R.id.pmIncome);

        this.paymentView = this.findViewById(R.id.paymentView);
        this.tvRequestPaymentToggle = this.findViewById(R.id.tvRequestPaymentToggle);
        this.tvRequestPaymentToggle.setOnClickListener(this);
        String[] paymentMethods = getContext().getResources().getStringArray(R.array.paymentMethods);
        ArrayAdapter<String> spnAdapter = new ArrayAdapter<>(getContext(), R.layout.spinner_item, paymentMethods);
        this.spnPaymentMethod = this.findViewById(R.id.spnPaymentMethod);
        this.spnPaymentMethod.setOnItemSelectedListener(this);
        this.spnPaymentMethod.setAdapter(spnAdapter);
        this.etAccountNumber = this.findViewById(R.id.etAccountNumber);
        this.etAmount = this.findViewById(R.id.etAmount);
        this.btnSendRequest = this.findViewById(R.id.btnSendRequest);

        this.btnSendRequest.setOnClickListener(this);

        this.loadUserRevenue();
    }

    @Subscribe
    public void onUserRevLoaded(RevenueLoadEvent event) {
        if (!event.isSuccess()) return;
        UserRev userRev = event.getUserRev();
        Pref.savePreference(getContext(), Pref.KEY_USER_REV_JSON, Commons.buildGson().toJson(userRev));
        this.updateViews(userRev);
    }

    private void updateViews(UserRev userRev) {
        if (userRev.isCanRequestForPayment())
            this.paymentView.setVisibility(VISIBLE);
        this.lastPaymentAmount.setText(String.valueOf("৳" + userRev.getLastPaymentAmount()));
        this.pmIncome.setText("৳" + String.valueOf(userRev.getPreviousMonthIncome()));
        this.cmIncome.setText("৳" + String.valueOf(userRev.getCurrentBalance()));
        this.cmInterationPoints.setText(String.valueOf(userRev.getCurrentMonthInteractionPoints()));
    }

    @Subscribe
    public void onUserLogin(LoginEvent event) {
        if (event.isSuccess())
            this.loadUserRevenue();
    }

    @Subscribe
    public void onEventListPostEvent(PostEventsEvent event) {
        if (event.isSuccess())
            this.loadUserRevenue();
    }

    private void loadUserRevenue() {
        // update user rev by default
        if (!Pref.isNull(getContext(), Pref.KEY_USER_REV_JSON)) {
            UserRev userRev = Commons.buildGson().fromJson(Pref.getPreferenceString(getContext(), Pref.KEY_USER_REV_JSON), UserRev.class);
            this.updateViews(userRev);
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        String month = new SimpleDateFormat("MMMM", Locale.US).format(calendar.getTime());
        String year = String.valueOf(calendar.get(Calendar.YEAR));
        ApiClient.loadUserRevenue(getContext(), month.trim().toLowerCase(), year);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.btnSendRequest) {
            if (!Validator.isValidRequestInformations(getContext(), this.spnPaymentMethod, this.etAccountNumber, this.etAmount))
                return;
            ApiClient.sendPaymentRequest(getContext(), this.paymentMethod, this.etAccountNumber.getText().toString(), this.etAmount.getText().toString());
        } else if (id == R.id.tvRequestPaymentToggle) {
            this.toggleRequestFormVisibility();
        }
    }

    @Subscribe
    public void onPaymentRequestSendEvent(PaymentRequestEvent event) {
        if (event.isSuccess()) {
            UIUtils.showDialog(getContext(), "Success!", "Successfully sent a payment request!");
            this.paymentView.setVisibility(GONE);
        } else
            UIUtils.showDialog(getContext(), "Error!!", event.getErrorMessage());
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
        this.paymentMethod = getResources().getStringArray(R.array.paymentMethods)[position];
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    private void toggleRequestFormVisibility() {
        if (this.spnPaymentMethod.getVisibility() == View.GONE)
            this.spnPaymentMethod.setVisibility(View.VISIBLE);
        else this.spnPaymentMethod.setVisibility(View.GONE);
        if (this.etAmount.getVisibility() == View.GONE)
            this.etAmount.setVisibility(View.VISIBLE);
        else this.etAmount.setVisibility(View.GONE);
        if (this.etAccountNumber.getVisibility() == View.GONE)
            this.etAccountNumber.setVisibility(View.VISIBLE);
        else this.etAccountNumber.setVisibility(View.GONE);
        if (this.btnSendRequest.getVisibility() == View.GONE)
            this.btnSendRequest.setVisibility(View.VISIBLE);
        else this.btnSendRequest.setVisibility(View.GONE);
    }
}