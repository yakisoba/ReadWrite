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

	/** TAG�̂Precord�ڂ̓X��� */
	public String ShopName;
	public String ShopTel;
	public String ShopAddress;
	public int DisplayType;

	/** TAG��2record�ȍ~�̃N�[�|����� */
	public String Coupon;
	public int CouponUseCount;
	public int CouponDispCount;

	/** �J�[�h��� */
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
					// 1record�ڂ�shop���
					NdefRecord[] records = msg.getRecords();
					listrecord = records;

					String output = parseTextRecord(records[0]);

					// String �𕪊�
					str1Ary = output.split("./");
					this.ShopName = str1Ary[0];
					this.ShopTel = str1Ary[1];
					this.ShopAddress = str1Ary[2];
					this.DisplayType = Integer.parseInt(str1Ary[3]);

					// ����record���烉���_���Ŏ擾
					Random rnd = new Random();
					RecordNum = rnd.nextInt(records.length - 1) + 1;
					output = parseTextRecord(records[RecordNum]);

					// string�𕪊�
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
					// ���ڑ���������ڑ�.
					if (!ndef.isConnected()) {
						ndef.connect();
					}
					// �����\���`�F�b�N
					if (ndef.isWritable()) {
						// ����
						ndef.writeNdefMessage(ndefmessage);
						Log.d("NFC", "�������ݐ���");
					} else {
						Log.d("NFC", "�������ݕs��");
					}
				} finally {
					ndef.close();
				}
			}
		} catch (FormatException e) {
			Log.d("NFC", "�������ݎ��s");
		} catch (IOException e) {
			Log.d("NFC", "�������ݎ��s");
		}
	}

	/**
	 * AndroidSDK�t���� ApiDemos �� ForegroundNdefPush ��蔲��. RTD �� Text
	 * ��NdefRecord���쐬����.
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
