function getCsrfHeaders() {
    const csrfToken  = document.querySelector('meta[name="_csrf"]');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]');

    
    if(!csrfToken || !csrfHeader) {
        return {};
    }

    console.log('csrfToken:{}',csrfToken);
    console.log('csrfHeader:{}',csrfHeader);    
    return {
        [csrfHeader.content]:csrfToken.content
    }
}

/*function testCsrf() {
    return {
        "Accept": "application/json",
        ...getCsrfHeaders()
    }
}

function testCsrf2() {
    return {
        "Accept": "application/json",
        "csrf": getCsrfHeaders()
    }
}*/

/**
 * Fetch 공통 요청 함수
 *
 * @param {string} url 요청 URL
 * @param {RequestInit} options Fetch 옵션
 * @returns {Promise<Object|string|null>}
 */
async function requestFetch(url, options = {}) {

    const response = await fetch(url, options);

    console.log("response:", response);
    console.log("status:", response.status);
    console.log("ok:", response.ok);

    // response.ok === true  : 200 ~ 299
    // response.ok === false : 그 외 상태 코드
    if (!response.ok) {

        let message = `HTTP Error : ${response.status}`;

        try {
            // 서버가 전송한 JSON 오류 응답
            const errorData = await response.json();

            // 오류 메시지 우선순위
            message =
                errorData.message
                || errorData.detail
                || message;

        } catch (ignore) {
            // JSON 형식의 오류 응답이 아닌 경우
        }

        throw new Error(message);
    }

    // 204 No Content
    if (response.status === 204) {
        return null;
    }

    const contentType =
        response.headers.get("content-type") || "";

    // JSON 응답
    if (contentType.includes("application/json")) {
        return response.json();
    }

    // text/plain, text/html 등의 문자열 응답
    return response.text();
}


/**
 * POST 요청
 *
 * @param {string} url 요청 URL
 * @param {Object} data 전송 데이터
 */
async function requestPost(url, data) {

    return requestFetch(url, {
        method: "POST",

        headers: {
            "Accept": "application/json",
            "Content-Type": "application/json",
            ...getCsrfHeaders()
            
        },

        body: JSON.stringify(data)
    });
}


/**
 * GET 요청
 *
 * @param {string} url 요청 URL
 * @param {Object|null} params Query String 데이터
 */
async function requestGet(url, params = null) {

    let requestUrl = url;

    if (params) {

        const queryString =
            new URLSearchParams(params).toString();

        console.log("queryString:", queryString);
       
        if (queryString) {
            const separator =
                url.includes("?") ? "&" : "?";

            requestUrl =
                `${url}${separator}${queryString}`;
        }
    }

    console.log("requestUrl:", requestUrl);
    
    return requestFetch(requestUrl, {
        method: "GET",

        headers: {
            "Accept": "application/json",
            ...getCsrfHeaders()
        }
    });
}

/**
 * Form 전송용 함수
 */
async function requestPostForm(url, data) {

    const formData =
        new URLSearchParams(); //Form 만드는거라고 생각해

    Object.entries(data)
        .forEach(([key, value]) => {

            if (
                value !== null
                && value !== undefined
            ) {
                formData.append(key, value);
            }
        });

    return requestFetch(url, {
        method: "POST",

        headers: {
            "Accept": "application/json",
            "Content-Type":
                "application/x-www-form-urlencoded;charset=UTF-8",
            ...getCsrfHeaders()
        },

        body: formData.toString()
    });
}