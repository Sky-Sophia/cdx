(() => {
    "use strict";

    function initPaginationJump() {
        document.addEventListener("submit", (event) => {
            const form = event.target.closest(".pagination-jump[data-base-url]");
            if (!form) {
                return;
            }
            event.preventDefault();

            const baseUrl = form.getAttribute("data-base-url") || "";
            const paramName = form.getAttribute("data-param-name") || "page";
            const maxPage = parseInt(form.getAttribute("data-max"), 10) || 1;
            const input = form.querySelector("[name='jumpPage']");
            let page = parseInt(input ? input.value : "", 10);
            if (Number.isNaN(page) || page < 1) {
                page = 1;
            }
            if (page > maxPage) {
                page = maxPage;
            }
            const sep = baseUrl.includes("?") ? "&" : "?";
            window.location.href = `${baseUrl}${sep}${paramName}=${page}`;
        });
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initPaginationJump, { once: true });
    } else {
        initPaginationJump();
    }
})();

