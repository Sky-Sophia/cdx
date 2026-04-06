(() => {
    "use strict";

    const OPEN_CLASS = "is-open";
    const SELECTED_CLASS = "is-selected";
    const HIGHLIGHTED_CLASS = "is-highlighted";

    /** Open a dropdown */
    function open(wrapper) {
        closeAll(wrapper);
        wrapper.classList.add(OPEN_CLASS);
        const trigger = wrapper.querySelector(".custom-select-trigger");
        if (trigger) trigger.setAttribute("aria-expanded", "true");
    }

    /** Close a dropdown */
    function close(wrapper) {
        wrapper.classList.remove(OPEN_CLASS);
        const trigger = wrapper.querySelector(".custom-select-trigger");
        if (trigger) trigger.setAttribute("aria-expanded", "false");
        clearHighlight(wrapper);
    }

    /** Close every open dropdown except `except` */
    function closeAll(except) {
        document.querySelectorAll(".custom-select.is-open").forEach((el) => {
            if (el !== except) close(el);
        });
    }

    /** Select an option */
    function selectOption(wrapper, option) {
        const panel = wrapper.querySelector(".custom-select-panel");
        const hiddenInput = wrapper.querySelector("input[type=hidden]");
        const textEl = wrapper.querySelector(".custom-select-trigger-text");

        // Mark selected
        panel.querySelectorAll(".custom-select-option").forEach((opt) =>
            opt.classList.remove(SELECTED_CLASS)
        );
        option.classList.add(SELECTED_CLASS);

        // Update hidden value
        const value = option.getAttribute("data-value") ?? "";
        if (hiddenInput) hiddenInput.value = value;

        if (textEl) {
            textEl.textContent = option.getAttribute("data-label") || option.textContent.trim();
        }

        close(wrapper);
    }

    /** Clear keyboard highlight */
    function clearHighlight(wrapper) {
        wrapper.querySelectorAll("." + HIGHLIGHTED_CLASS).forEach((el) =>
            el.classList.remove(HIGHLIGHTED_CLASS)
        );
    }

    /** Get all visible options */
    function getOptions(wrapper) {
        return Array.from(
            wrapper.querySelectorAll(".custom-select-option")
        );
    }

    /** Keyboard navigation */
    function handleKeyboard(e, wrapper) {
        const options = getOptions(wrapper);
        if (!options.length) return;

        const current = wrapper.querySelector("." + HIGHLIGHTED_CLASS);
        let idx = current ? options.indexOf(current) : -1;

        if (e.key === "ArrowDown") {
            e.preventDefault();
            idx = (idx + 1) % options.length;
        } else if (e.key === "ArrowUp") {
            e.preventDefault();
            idx = (idx - 1 + options.length) % options.length;
        } else if (e.key === "Enter") {
            e.preventDefault();
            if (current) {
                selectOption(wrapper, current);
            }
            return;
        } else if (e.key === "Escape") {
            e.preventDefault();
            close(wrapper);
            return;
        } else {
            return;
        }

        clearHighlight(wrapper);
        options[idx].classList.add(HIGHLIGHTED_CLASS);
        options[idx].scrollIntoView({ block: "nearest" });
    }

    // ── Event delegation ──────────────────────────────────

    // Initialize: sync trigger text from pre-selected option (server-rendered)
    function initAll() {
        document.querySelectorAll(".custom-select").forEach((wrapper) => {
            const selected = wrapper.querySelector(".custom-select-option.is-selected");
            if (selected) {
                const textEl = wrapper.querySelector(".custom-select-trigger-text");
                if (textEl) {
                    textEl.textContent = selected.getAttribute("data-label") || selected.textContent.trim();
                }
            }
        });
    }

    // Run init on DOMContentLoaded
    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initAll);
    } else {
        initAll();
    }

    // Toggle on trigger click
    document.addEventListener("click", (e) => {
        const trigger = e.target.closest(".custom-select-trigger");
        if (trigger) {
            e.preventDefault();
            const wrapper = trigger.closest(".custom-select");
            if (!wrapper) return;
            if (wrapper.classList.contains(OPEN_CLASS)) {
                close(wrapper);
            } else {
                open(wrapper);
            }
            return;
        }

        // Option click
        const option = e.target.closest(".custom-select-option");
        if (option) {
            const wrapper = option.closest(".custom-select");
            if (wrapper) selectOption(wrapper, option);
            return;
        }

        // Click outside — close all
        closeAll(null);
    });

    // Keyboard
    document.addEventListener("keydown", (e) => {
        const openDropdown = document.querySelector(".custom-select.is-open");
        if (openDropdown) {
            handleKeyboard(e, openDropdown);
        }
    });
})();

