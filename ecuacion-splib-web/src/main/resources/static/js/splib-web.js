/*
 * Copyright © 2012 ecuacion.jp (info@ecuacion.jp)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
function addNumberFormatFunction(itemName) {
	var object = document.getElementById(itemName);
	
	object.addEventListener('focus', function() {
		object.value = changeCurrencyToNumber(object.value);
	});

	object.addEventListener('blur', function() {
		object.value = changeNumberToCurrency(object.value);
	});
}

function changeNumberToCurrency(number){
	var i;
	var savedNumber = number;
	var currencyFormat = "";
	
	// 一度カンマ入りの通貨表示を数値表示に戻す
	number = changeCurrencyToNumber(number);

	// マイナス記号を含めて3文字とすると、「-,100,000」のようになってしまうので、
	// マイナスは抜いて、3桁以上あるかを判断する
	if (number.replace("-", "").length > 3) {
		// 3桁ずつ区切って、配列に入れていく
		for (i = 0; i < 10; i++) {
			if (number.replace("-", "").length > 3) {
				// 最初以外は、区切りとしてカンマを入れる
				if (i == 0) {
					currencyFormat = number.substring(number.length-3);
				} else {
					currencyFormat = number.substring(number.length-3) + "," + currencyFormat;
				}
				// 元の数値は、下3桁を削る
				number = number.substring(0, number.length-3);
			}else {
				break;
			}
		}
		// 最後に、3桁以内の数値を頭につける
		currencyFormat = number + "," + currencyFormat;
	} else {
		currencyFormat = number;
	}
	return currencyFormat;
}

function changeCurrencyToNumber(currency){
	while(true) {
		if (currency.search(",") >= 0) {
			currency = currency.replace(",", "");
		} else {
			return currency;
		}
	}
}

function doubleClickPreventionLockButtonsOnSubmit(event, form) {
	
	const buttons = form.querySelectorAll('button, input[type="submit"]');
	buttons.forEach(button => {
		if (button.dataset.disabledOnSubmit) {
			if (button === event.submitter) {
				form.elements['action'].value = event.submitter.name;
				event.submitter.innerText = form.dataset.submittingMessage;
			}
			
			button.disabled = true;
		}
	});
}

function doubleClickPreventionUnlockButtonsOnBrowserBack(window) {

	window.addEventListener('pageshow', function(event) {
		// Means 'if the page is open by browser back'.
		if (event.persisted || window.performance && window.performance.navigation.type === 2) {
			Array.from(document.forms).forEach(form => {
				const buttons = form.querySelectorAll('button, input[type="submit"]');
				buttons.forEach(button => {
					if (button.dataset.disabledOnSubmit) {
						button.disabled = false;
						button.innerText = button.dataset.originalLabel;
					}
				});
			});
		}
	});
}

/** Defined as an property of a window to be called from window[]. */
window.waitForDownloadToFinish = function(_event, _button) {
	const intervalId = setInterval(function() {
		let cookie = document.cookie;
		if (cookie.includes("download_status=completed")) {
			clearInterval(intervalId);
			// delete cookie
			document.cookie = 'download_status=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;';
			window.location.href = './page?success';
		}
	}, 1000);
};
