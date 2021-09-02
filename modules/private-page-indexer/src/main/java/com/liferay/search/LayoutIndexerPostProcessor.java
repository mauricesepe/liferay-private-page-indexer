package com.liferay.search;

import com.liferay.search.crawler.configuration.CrawlerConfiguration;
import com.liferay.fragment.constants.FragmentEntryLinkConstants;
import com.liferay.fragment.model.FragmentEntryLink;
import com.liferay.fragment.renderer.DefaultFragmentRendererContext;
import com.liferay.fragment.renderer.FragmentRendererController;
import com.liferay.fragment.service.FragmentEntryLinkLocalService;
import com.liferay.petra.encryptor.Encryptor;
import com.liferay.petra.encryptor.EncryptorException;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.search.BooleanQuery;
import com.liferay.portal.kernel.search.Document;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.search.IndexerPostProcessor;
import com.liferay.portal.kernel.search.SearchContext;
import com.liferay.portal.kernel.search.Summary;
import com.liferay.portal.kernel.search.filter.BooleanFilter;
import com.liferay.portal.kernel.service.CompanyLocalService;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.service.LayoutLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextThreadLocal;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.Html;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.UnicodeFormatter;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.staging.StagingGroupHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

@Component(
	immediate = true,
	configurationPid = "com.ge.search.crawler.configuration.CrawlerConfiguration",
	property = {
		"indexer.class.name=com.liferay.portal.kernel.model.Layout"
	},
	service = IndexerPostProcessor.class)
public class LayoutIndexerPostProcessor implements IndexerPostProcessor {

	@Override
	public void postProcessContextBooleanFilter(
		BooleanFilter booleanFilter, SearchContext searchContext)
		throws Exception {

	}

	@Override
	public void postProcessDocument(Document document, Object obj)
		throws Exception {

		Layout layout = (Layout) obj;

		if (!layout.isPrivateLayout() || layout.isHidden() ||
			layout.isSystem()) {
			return;
		}

		long groupId = layout.getGroupId();

		List<FragmentEntryLink> fragmentEntryLinks =
			_fragmentEntryLinkLocalService.getFragmentEntryLinksByPlid(
				groupId, layout.getPlid());

		if ((fragmentEntryLinks == null) || fragmentEntryLinks.isEmpty()) {
			return;
		}

		Set<Locale> locales =
			LanguageUtil.getAvailableLocales(groupId);
		String[] layoutAvailableLanguageIds = layout.getAvailableLanguageIds();

		HttpServletRequest request = null;
		HttpServletResponse response = null;
		ServiceContext serviceContext =
			ServiceContextThreadLocal.getServiceContext();

		if ((serviceContext != null) && (serviceContext.getRequest() != null)) {
			request = serviceContext.getRequest();
			response = serviceContext.getResponse();
		}

		long companyId = layout.getCompanyId();

		boolean allowAuthenticate = _crawlerConfiguration.autoAthenticate();

		LayoutCrawler layoutCrawler = null;

		if (allowAuthenticate) {
			String crawlerUserIid = _crawlerConfiguration.crawlerId();
			String password = _crawlerConfiguration.crawlerPassword();
			Company company = _companyLocalService.getCompany(companyId);

			long userId = _decryptUserId(crawlerUserIid, company);

			if (userId == 0 ||
				_userLocalService.hasGroupUser(groupId, userId)) {
					layoutCrawler = new LayoutCrawler(
					company, _portal, crawlerUserIid, password);
			}
			else {
				if (_log.isWarnEnabled()) {
					_log.warn(
						"Crawler user " + userId + " is not member of group " +
							groupId);
				}
			}
		}

		for (Locale locale : locales) {

			if (!ArrayUtil.contains(
				layoutAvailableLanguageIds, LocaleUtil.toLanguageId(locale))) {

				return;
			}

			String content = "";

			if (allowAuthenticate && layoutCrawler != null) {
				content = _crawlContent(layout, layoutCrawler, locale);
			}
			else if (request != null && response != null) {
				content = _renderFragments(
					fragmentEntryLinks, request, response, locale);
			}

			content = _html.stripHtml(content);

			if (Validator.isNull(content)) {

				if (_log.isDebugEnabled()) {
					_log.debug(
						"No content extracted for indexing layout " +
							layout.getFriendlyURL());
				}

				continue;
			}

			document.addText(
				Field.getLocalizedName(locale, Field.CONTENT), content);
		}
	}

	@Override
	public void postProcessFullQuery(
		BooleanQuery fullQuery, SearchContext searchContext)
		throws Exception {

	}

	@Override
	public void postProcessSearchQuery(
		BooleanQuery searchQuery, BooleanFilter booleanFilter,
		SearchContext searchContext)
		throws Exception {

	}

	@Override
	public void postProcessSummary(
		Summary summary, Document document, Locale locale, String snippet) {

		int maxlength = _crawlerConfiguration.searchSummaryMaxLength();

		if (maxlength > 0) {
			summary.setMaxContentLength(maxlength);
		}
	}

	@Activate
	@Modified
	protected void activate(Map<Object, Object> properties) {

		_crawlerConfiguration = ConfigurableUtil.createConfigurable(
			CrawlerConfiguration.class, properties);
	}

	private String _crawlContent(
		Layout layout, LayoutCrawler layoutCrawler, Locale locale) {

		return _getWrapper(layoutCrawler.getLayoutContent(layout, locale));
	}

	private long _decryptUserId(String id, Company company)
		throws EncryptorException {

		String userIdString = new String(UnicodeFormatter.hexToBytes(id));

		userIdString = Encryptor.decrypt(company.getKeyObj(), userIdString);

		long userId = GetterUtil.getLong(userIdString);
		return userId;
	}

	private String _getWrapper(String layoutContent) {
		int wrapperIndex = layoutContent.indexOf(_WRAPPER_ELEMENT);

		if (wrapperIndex == -1) {
			return layoutContent;
		}

		return layoutContent.substring(
			wrapperIndex + _WRAPPER_ELEMENT.length());
	}

	private String _renderFragmentEntryLink(
			FragmentEntryLink fragmentEntryLink, Locale locale,
			HttpServletRequest request, HttpServletResponse response)
		throws PortalException {

		FragmentRendererController fragmentRendererController =
			_fragmentRendererController;

		DefaultFragmentRendererContext defaultFragmentRendererContext =
			new DefaultFragmentRendererContext(fragmentEntryLink);

		defaultFragmentRendererContext.setFieldValues(new HashMap<String, Object>());
		defaultFragmentRendererContext.setLocale(locale);
		defaultFragmentRendererContext.setMode(FragmentEntryLinkConstants.VIEW);
		defaultFragmentRendererContext.setSegmentsExperienceIds(
			new long[0]);

		return fragmentRendererController.render(
			defaultFragmentRendererContext, request,
			response);
	}

	private String _renderFragments(
			List<FragmentEntryLink> fragmentEntryLinks, HttpServletRequest request,
			HttpServletResponse response, Locale locale)
		throws PortalException {

		String content;
		StringBundler sb = new StringBundler(fragmentEntryLinks.size());

		for (FragmentEntryLink fragmentEntryLink : fragmentEntryLinks) {

			String renderFragmentEntry = _renderFragmentEntryLink(
				fragmentEntryLink, locale, request, response);

			sb.append(renderFragmentEntry);
		}

		content = sb.toString();
		return content;
	}

	private static Log _log =
		LogFactoryUtil.getLog(LayoutIndexerPostProcessor.class);

	private static final String _WRAPPER_ELEMENT = "id=\"content\">";

	@Reference
	private CompanyLocalService _companyLocalService;

	private CrawlerConfiguration _crawlerConfiguration;

	@Reference
	private FragmentEntryLinkLocalService _fragmentEntryLinkLocalService;

	@Reference
	private FragmentRendererController _fragmentRendererController;

	@Reference
	private GroupLocalService _groupLocalService;

	@Reference
	private Html _html;

	@Reference
	private LayoutLocalService _layoutLocalService;

	@Reference
	private Portal _portal;

	@Reference
	private StagingGroupHelper _stagingGroupHelper;

	@Reference
	private UserLocalService _userLocalService;
}
