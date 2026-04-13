(() => {
    "use strict";

    const FIELD_SELECTOR = "input, textarea";
    const FORM_SELECTOR = "form";

    function disableFieldAutocomplete(field) {
        if (!field || typeof field.setAttribute !== "function") {
            return;
        }

        field.setAttribute("autocomplete", "off");
        field.setAttribute("autocorrect", "off");
        field.setAttribute("autocapitalize", "off");
        field.setAttribute("spellcheck", "false");
    }

    function disableFormAutocomplete(form) {
        if (!form || typeof form.setAttribute !== "function") {
            return;
        }

        form.setAttribute("autocomplete", "off");
    }

    function disableAutocomplete(root) {
        const scope = root && typeof root.querySelectorAll === "function" ? root : document;

        if (scope.matches && scope.matches(FORM_SELECTOR)) {
            disableFormAutocomplete(scope);
        }
        if (scope.matches && scope.matches(FIELD_SELECTOR)) {
            disableFieldAutocomplete(scope);
        }

        scope.querySelectorAll(FORM_SELECTOR).forEach(disableFormAutocomplete);
        scope.querySelectorAll(FIELD_SELECTOR).forEach(disableFieldAutocomplete);
    }

    function init() {
        disableAutocomplete(document);

        const observer = new MutationObserver((mutations) => {
            mutations.forEach((mutation) => {
                mutation.addedNodes.forEach((node) => {
                    if (node.nodeType === Node.ELEMENT_NODE) {
                        disableAutocomplete(node);
                    }
                });
            });
        });

        observer.observe(document.documentElement, {
            childList: true,
            subtree: true
        });
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init, { once: true });
    } else {
        init();
    }
})();
