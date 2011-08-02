package test.readwrite;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class Coupon extends Activity {

	NfcAdapter mAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		Log.d("NFC", "start");

		mAdapter = NfcAdapter.getDefaultAdapter(this);
		super.onCreate(savedInstanceState);

		CouponData couponData = new CouponData(getIntent());

		if (couponData.tag != null) {
			switch (couponData.DisplayType) {
			case 0:
				setContentView(R.layout.coupon);
				break;
			case 1:
				setContentView(R.layout.coupon1);
				break;
			default:
				setContentView(R.layout.coupon);
				break;
			}

			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			TextView coupon = (TextView) findViewById(R.id.coupon);
			TextView shop = (TextView) findViewById(R.id.shop);
			coupon.setText(couponData.Coupon);
			shop.setText(couponData.ShopName + "\n" + couponData.ShopTel + "\n"
					+ couponData.ShopAddress);

			Log.d("NFC", "店" + couponData.ShopName + " " + couponData.ShopTel
					+ " " + couponData.ShopAddress);
			Log.d("NFC", "クーポン" + couponData.Coupon + " "
					+ couponData.CouponDispCount);

			// 読み込んだrecordデータを書き込みする。
			couponData.writeNdefMessage(couponData.tag);
		}
	}
}