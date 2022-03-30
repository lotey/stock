var config = {
	mode: "fixed_servers",
	rules: {
		singleProxy: {
			scheme: "https",
			host: "{host}",
			port: {port}
		},
		bypassList: ["foobar.com"]
	}
  };
chrome.proxy.settings.set({value: config, scope: "regular"}, function() {});
function callbackFn(details) {
	return {
		authCredentials: {
			username: "{userName}",
			password: "{password}"
		}
	};
}
chrome.webRequest.onAuthRequired.addListener(
	callbackFn,
	{urls: ["<all_urls>"]},
	['blocking']
);