
package com.liferay.search;

import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.CookieKeys;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.Portal;

import java.net.InetAddress;
import java.util.Locale;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.util.EntityUtils;

public class LayoutCrawler {

	public LayoutCrawler(
		Company company, Portal portal, String id, String password) {

		this._company = company;
		this._portal = portal;

		_inetAddress = _getPortalServerInetAddress();
		_hostName = _inetAddress.getHostAddress();
		_portalServerPort = _getPortalServerPort();

		HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();

		_httpClient = httpClientBuilder.setUserAgent(_USER_AGENT).build();

		initializeCredentials(id, password);
	}

	public String getLayoutContent(Layout layout, Locale locale) {

		try {

			ThemeDisplay themeDisplay =
				_getThemeDisplay(layout, locale, _portalServerPort, _hostName);

			HttpClientContext httpClientContext =
				_getBasicHttpClienContext(_hostName);

			BasicClientCookie guestLanguageIdClientCookie = _createClientCookie(
				CookieKeys.GUEST_LANGUAGE_ID, LocaleUtil.toLanguageId(locale),
				_hostName);

			httpClientContext.getCookieStore().addCookie(
				guestLanguageIdClientCookie);

			String layoutFullURL = _portal.getLayoutFullURL(layout, themeDisplay);

			if (_log.isDebugEnabled()) {
				_log.debug("layoutFullURL " + layoutFullURL);
			}

			HttpGet httpGet =
				new HttpGet(layoutFullURL);

			HttpResponse httpResponse =
				_httpClient.execute(httpGet, httpClientContext);

			StatusLine statusLine = httpResponse.getStatusLine();

			if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
				return EntityUtils.toString(httpResponse.getEntity());
			}
			else {
				if (_log.isWarnEnabled()) {
					_log.warn("HttpStatus " + statusLine.getStatusCode());
				}
			}
		}
		catch (Exception exception) {
			if (_log.isWarnEnabled()) {
				_log.warn("Unable to crawl layout content", exception);
			}
		}

		return StringPool.BLANK;
	}

	protected void initializeCredentials(String id, String password) {

		_autoUserId = id;
		_autoPassword = password;
	}

	private BasicClientCookie _createClientCookie(
		String cookieName, String cookieValue, String domain) {

		BasicClientCookie basicClientCookie =
			new BasicClientCookie(cookieName, cookieValue);

		basicClientCookie.setDomain(domain);

		return basicClientCookie;
	}

	private HttpClientContext _getBasicHttpClienContext(String hostName) {

		if (_httpClientContext != null) {
			return _httpClientContext;
		}

		CookieStore cookieStore = new BasicCookieStore();

		BasicClientCookie autoIdClientCookie =
			_createClientCookie(CookieKeys.ID, _autoUserId, hostName);
		BasicClientCookie autoPasswordClientCookie =
			_createClientCookie(CookieKeys.PASSWORD, _autoPassword, hostName);
		BasicClientCookie rememberMeClientCookie =
			_createClientCookie(CookieKeys.REMEMBER_ME, _rememberMe, hostName);

		cookieStore.addCookie(autoIdClientCookie);
		cookieStore.addCookie(autoPasswordClientCookie);
		cookieStore.addCookie(rememberMeClientCookie);

		HttpClientContext httpClientContext = new HttpClientContext();

		httpClientContext.setCookieStore(cookieStore);

		_httpClientContext = httpClientContext;

		return httpClientContext;
	}

	private InetAddress _getPortalServerInetAddress() {

		if (_inetAddress != null) {
			return _inetAddress;
		}

		_inetAddress = _portal.getPortalServerInetAddress(false);

		return _inetAddress;
	}

	private int _getPortalServerPort() {

		if (_portalServerPort > 0) {
			return _portalServerPort;
		}

		_portalServerPort = _portal.getPortalServerPort(false);

		return _portalServerPort;
	}

	private ThemeDisplay _getThemeDisplay(
		Layout layout, Locale locale, int portalServerPort, String hostName)
		throws PortalException {

		ThemeDisplay themeDisplay = new ThemeDisplay();

		themeDisplay.setCompany(_company);
		themeDisplay.setLanguageId(LocaleUtil.toLanguageId(locale));
		themeDisplay.setLayout(layout);
		themeDisplay.setLayoutSet(layout.getLayoutSet());
		themeDisplay.setLocale(locale);
		themeDisplay.setScopeGroupId(layout.getGroupId());
		themeDisplay.setServerName(hostName);

		themeDisplay.setServerPort(portalServerPort);
		themeDisplay.setSiteGroupId(layout.getGroupId());

		return themeDisplay;
	}

	public static final String COOKIE_KEYS_CRAWLER_HASH =
		"COOKIE_KEYS_CRAWLER_HASH";

	private static final Log _log =
		LogFactoryUtil.getLog(LayoutCrawler.class);

	private static final String _rememberMe = Boolean.toString(true);

	private static final String _USER_AGENT = "Liferay Page Crawler";

	private String _autoPassword;

	private String _autoUserId;

	private Company _company;

	private String _hostName;

	private HttpClient _httpClient;

	private HttpClientContext _httpClientContext;

	private InetAddress _inetAddress;

	private Portal _portal;

	private int _portalServerPort;
}
