package test.readwrite;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;

import android.content.Intent;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Parcelable;
import android.util.Log;

public class CouponData {
	NdefRecord[] listrecord;

	/** TAGの１record目の店情報 */
	public String ShopName;
	public String ShopTel;
	public String ShopAddress;
	public int DisplayType;

	/** TAGの2record以降のクーポン情報 */
	public String Coupon;
	public int CouponUseCount;
	public int CouponDispCount;

	/** カード情報 */
	public Tag tag;
	public String size;
	public String type;
	public int RecordNum;

	public CouponData(Intent intent) {
		this.tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

		if (tag != null) {
			String[] techList = tag.getTechList();
			for (String tech : techList) {
				if (Ndef.class.getName().equals(tech)) {
					ndefread(intent);
				}
			}
		}
	}

	private void ndefread(Intent intent) {
		String[] str1Ary;

		try {
			Parcelable[] rawMsgs = intent
					.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
			NdefMessage[] msgs = null;

			if (rawMsgs != null) {
				msgs = new NdefMessage[rawMsgs.length];
				for (int i = 0; i < rawMsgs.length; i++) {
					msgs[i] = (NdefMessage) rawMsgs[i];
				}
			}

			if (msgs != null) {
				for (NdefMessage msg : msgs) {
					// 1record目はshop情報
					NdefRecord[] records = msg.getRecords();
					listrecord = records;

					String output = parseTextRecord(records[0]);

					// String を分割
					str1Ary = output.split("./");
					this.ShopName = str1Ary[0];
					this.ShopTel = str1Ary[1];
					this.ShopAddress = str1Ary[2];
					this.DisplayType = Integer.parseInt(str1Ary[3]);

					// 他のrecordからランダムで取得
					Random rnd = new Random();
					RecordNum = rnd.nextInt(records.length - 1) + 1;
					output = parseTextRecord(records[RecordNum]);

					// stringを分割
					str1Ary = output.split("./");
					this.Coupon = str1Ary[0];
					this.CouponDispCount = Integer.parseInt(str1Ary[1]);
				}
			}
		} catch (Exception e) {
			Log.d("NFC", "ndefread" + e.toString());
		}
	}

	private String parseTextRecord(NdefRecord record) {
		if (record.getTnf() != NdefRecord.TNF_WELL_KNOWN) {
			throw new IllegalArgumentException("unknown tnf");
		} else if (Arrays.equals(record.getType(), NdefRecord.RTD_TEXT)) {
			try {
				this.type = "text";
				byte[] payload = record.getPayload();
				this.size = payload.length + " byte";
				String textEncoding = ((payload[0] & 0x80) == 0) ? "UTF-8"
						: "UTF-16";
				int languageCodeLength = payload[0] & 0x3F;

				@SuppressWarnings("unused")
				String languageCode = new String(payload, 1,
						languageCodeLength, "US-ASCII");
				String text = new String(payload, languageCodeLength + 1,
						payload.length - languageCodeLength - 1, textEncoding);
				return text;
			} catch (Exception e) {
				throw new IllegalStateException("unsupported encoding", e);
			}
		} else {
			throw new IllegalArgumentException("unknown type");
		}
	}

	public void writeNdefMessage(Tag tag) {
		String message = Coupon + "./" + DisplaycountDecrement();
		listrecord[RecordNum] = newTextRecord(message, Locale.JAPANESE, true);
		NdefMessage ndefmessage = new NdefMessage(listrecord);

		try {
			if (Arrays.asList(tag.getTechList()).contains(Ndef.class.getName())) {
				Ndef ndef = Ndef.get(tag);
				try {
					// 未接続だったら接続.
					if (!ndef.isConnected()) {
						ndef.connect();
					}
					// 書込可能かチェック
					if (ndef.isWritable()) {
						// 書込
						ndef.writeNdefMessage(ndefmessage);
						Log.d("NFC", "書き込み成功");
					} else {
						Log.d("NFC", "書き込み不可");
					}
				} finally {
					ndef.close();
				}
			}
		} catch (FormatException e) {
			Log.d("NFC", "書き込み失敗");
		} catch (IOException e) {
			Log.d("NFC", "書き込み失敗");
		}
	}

	/**
	 * AndroidSDK付属の ApiDemos の ForegroundNdefPush より抜粋. RTD が Text
	 * のNdefRecordを作成する.
	 */
	public static NdefRecord newTextRecord(String text, Locale locale,
			boolean encodeInUtf8) {
		byte[] langBytes = locale.getLanguage().getBytes(
				Charset.forName("US-ASCII"));

		Charset utfEncoding = encodeInUtf8 ? Charset.forName("UTF-8") : Charset
				.forName("UTF-16");
		byte[] textBytes = text.getBytes(utfEncoding);

		int utfBit = encodeInUtf8 ? 0 : (1 << 7);
		char status = (char) (utfBit + langBytes.length);

		byte[] data = new byte[1 + langBytes.length + textBytes.length];
		data[0] = (byte) status;
		System.arraycopy(langBytes, 0, data, 1, langBytes.length);
		System.arraycopy(textBytes, 0, data, 1 + langBytes.length,
				textBytes.length);

		return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT,
				new byte[0], data);
	}

	public void setCoupon(String name, String tel, String add, String coupon,
			int usecount, int displaycount, int recordnum) {

		this.ShopName = name;
		this.ShopTel = tel;
		this.ShopAddress = add;
		this.Coupon = coupon;
		this.CouponUseCount = usecount;
		this.CouponDispCount = displaycount;
		this.RecordNum = recordnum;
	}

	public void UseCountIncrement() {
		this.CouponUseCount++;
	}

	public void UseCountDecrement() {
		if (CouponUseCount > 0) {
			this.CouponUseCount--;
		}
	}

	public void DisplayCountIncrement() {
		this.CouponDispCount++;
	}

	public int DisplaycountDecrement() {
		if (CouponDispCount > 0) {
			this.CouponDispCount--;
			return this.CouponDispCount;
		} else {
			return 0;
		}
	}

	public CouponData getCouponData() {
		return this;
	}
}
