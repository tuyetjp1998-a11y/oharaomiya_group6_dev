package bean;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 保護者の設定画面に表示する承認待ち申請。
 */
public class FamilyStudentRequest implements Serializable {

	private static final long serialVersionUID = 1L;

	private long id;
	private String studentLoginId;
	// 審査結果
	private String status;

	// 申請日時
	private OffsetDateTime requestedAt;

	// 審査日時
	private OffsetDateTime reviewedAt;

	public FamilyStudentRequest() {
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getStudentLoginId() {
		return studentLoginId;
	}

	public void setStudentLoginId(String studentLoginId) {
		this.studentLoginId = studentLoginId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public OffsetDateTime getRequestedAt() {
		return requestedAt;
	}

	public void setRequestedAt(OffsetDateTime requestedAt) {
		this.requestedAt = requestedAt;
	}

	public OffsetDateTime getReviewedAt() {
		return reviewedAt;
	}

	public void setReviewedAt(OffsetDateTime reviewedAt) {
		this.reviewedAt = reviewedAt;
	}

	public String getRequestedAtText() {
		return formatDateTime(requestedAt);
	}

	public String getReviewedAtText() {
		return formatDateTime(reviewedAt);
	}

	private String formatDateTime(OffsetDateTime value) {
		if (value == null) {
			return "";
		}

		return value.atZoneSameInstant(ZoneId.of("Asia/Tokyo"))
				.format(DateTimeFormatter.ofPattern(
						"yyyy/MM/dd HH:mm"));
	}
}