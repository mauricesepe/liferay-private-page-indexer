import com.liferay.petra.encryptor.Encryptor;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.CompanyLocalServiceUtil;
import com.liferay.portal.kernel.util.UnicodeFormatter;

// Grab from: Control Panel > Virtual Instances > Instance ID
long companyId = 20097;

// Grab from User details screen
long userId = 41365;

// The plain password of user above
String plainPassword = "123456";

try {
	Company company = CompanyLocalServiceUtil.getCompany(companyId);

	String hashId = Encryptor.encrypt(company.getKeyObj(), String.valueOf(userId));
	hashId = UnicodeFormatter.bytesToHex(hashId.getBytes());

	String hashPassword = Encryptor.encrypt(company.getKeyObj(), plainPassword);
	hashPassword = UnicodeFormatter.bytesToHex(hashPassword.getBytes());

	out.println("ID: " + hashId);
	out.println("password: " + hashPassword);
}
catch (Exception e) {
	out.println("Error: " + e.getMessage());
}