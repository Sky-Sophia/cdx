(() => {
    "use strict";

    const OPEN_CLASS = "is-open";
    const SELECTED_CLASS = "is-selected";
    const HIGHLIGHTED_CLASS = "is-highlighted";

    function resolveLabelText(field) {
        const label = field && typeof field.closest === "function" ? field.closest("label") : null;
        if (!label) {
            return "";
        }

        for (const node of label.childNodes) {
            if (node.nodeType === Node.TEXT_NODE) {
                const text = (node.textContent || "").replace(/\s+/g, " ").trim();
                if (text) {
                    return text.replace(/[：:]+$/, "");
                }
            }
        }

        return "";
    }

    function showValidationBubble(field, anchor, message) {
        document.dispatchEvent(new CustomEvent("validationbubble:show", {
            detail: {
                field,
                anchor,
                message
            }
        }));
    }

    function isRequired(wrapper) {
        return wrapper.hasAttribute("data-required") || wrapper.querySelector("input[type=hidden][required]") !== null;
    }

    function hasValue(wrapper) {
        const hiddenInput = wrapper.querySelector("input[type=hidden]");
        return !!(hiddenInput && hiddenInput.value !== "");
    }

    function syncInvalidState(wrapper) {
        const trigger = wrapper.querySelector(".custom-select-trigger");
        const invalid = isRequired(wrapper) && !hasValue(wrapper);
        wrapper.classList.toggle("is-invalid", invalid);
        if (trigger) {
            trigger.setAttribute("aria-invalid", invalid ? "true" : "false");
        }
        return !invalid;
    }

    function clearInvalidState(wrapper) {
        const trigger = wrapper.querySelector(".custom-select-trigger");
        wrapper.classList.remove("is-invalid");
        if (trigger) {
            trigger.setAttribute("aria-invalid", "false");
        }
    }

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
        const previousValue = hiddenInput ? hiddenInput.value : "";

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

        syncInvalidState(wrapper);

        if (hiddenInput && previousValue !== value) {
            hiddenInput.dispatchEvent(new Event("change", { bubbles: true }));
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
            clearInvalidState(wrapper);
        });

        document.querySelectorAll("form").forEach((form) => {
            form.addEventListener("submit", (event) => {
                const customSelects = Array.from(form.querySelectorAll(".custom-select"));
                const firstInvalid = customSelects.find((wrapper) => !syncInvalidState(wrapper));
                if (firstInvalid) {
                    event.preventDefault();
                    const hiddenInput = firstInvalid.querySelector("input[type=hidden]");
                    const trigger = firstInvalid.querySelector(".custom-select-trigger");
                    if (trigger) {
                        trigger.focus();
                    }
                    if (hiddenInput && trigger) {
                        const labelText = resolveLabelText(hiddenInput);
                        showValidationBubble(hiddenInput, trigger, labelText ? `请选择${labelText}` : "请选择此项");
                    }
                }
            });
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

(() => {
    "use strict";

    const PICKER_SELECTOR = "input[data-ui-picker]";

    function resolveLabelText(field) {
        const label = field && typeof field.closest === "function" ? field.closest("label") : null;
        if (!label) {
            return "";
        }

        for (const node of label.childNodes) {
            if (node.nodeType === Node.TEXT_NODE) {
                const text = (node.textContent || "").replace(/\s+/g, " ").trim();
                if (text) {
                    return text.replace(/[：:]+$/, "");
                }
            }
        }

        return "";
    }

    function showValidationBubble(field, anchor, message) {
        document.dispatchEvent(new CustomEvent("validationbubble:show", {
            detail: {
                field,
                anchor,
                message
            }
        }));
    }

    function pad(value) {
        return String(value).padStart(2, "0");
    }

    function parseDateValue(value, mode) {
        if (mode === "month") {
            if (!/^\d{4}-\d{2}$/.test(value)) {
                return null;
            }
            const [year, month] = value.split("-").map(Number);
            const date = new Date(year, month - 1, 1);
            return Number.isNaN(date.getTime()) || date.getFullYear() !== year || date.getMonth() !== month - 1
                ? null
                : date;
        }

        if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) {
            return null;
        }

        const [year, month, day] = value.split("-").map(Number);
        const date = new Date(year, month - 1, day);
        return Number.isNaN(date.getTime())
            || date.getFullYear() !== year
            || date.getMonth() !== month - 1
            || date.getDate() !== day
            ? null
            : date;
    }

    function formatInputValue(date, mode) {
        if (mode === "month") {
            return `${date.getFullYear()}-${pad(date.getMonth() + 1)}`;
        }
        return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
    }

    function formatDisplayValue(date, mode) {
        if (mode === "month") {
            return `${date.getFullYear()}/${pad(date.getMonth() + 1)}`;
        }
        return `${date.getFullYear()}/${pad(date.getMonth() + 1)}/${pad(date.getDate())}`;
    }

    function formatHeaderValue(date, mode) {
        if (mode === "month") {
            return `${date.getFullYear()}年`;
        }
        return `${date.getFullYear()}年${pad(date.getMonth() + 1)}月`;
    }

    function getPlaceholder(mode) {
        return mode === "month" ? "年/月" : "年/月/日";
    }

    function createIcon() {
        const icon = document.createElement("span");
        icon.className = "ui-picker-icon";
        icon.setAttribute("aria-hidden", "true");
        icon.innerHTML = "<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\"><rect x=\"3\" y=\"4\" width=\"18\" height=\"18\" rx=\"2\" ry=\"2\"></rect><line x1=\"16\" y1=\"2\" x2=\"16\" y2=\"6\"></line><line x1=\"8\" y1=\"2\" x2=\"8\" y2=\"6\"></line><line x1=\"3\" y1=\"10\" x2=\"21\" y2=\"10\"></line></svg>";
        return icon;
    }

    function updateInvalidState(wrapper, input, trigger, force = false) {
        const invalid = input.required && !input.value;
        if (force) {
            wrapper.dataset.validationShown = "true";
        }
        const showInvalid = invalid && (
            wrapper.dataset.validationShown === "true"
            || input.dataset.uiPickerInteracted === "true"
        );
        wrapper.classList.toggle("is-invalid", showInvalid);
        trigger.setAttribute("aria-invalid", showInvalid ? "true" : "false");
        return !invalid;
    }

    function buildPicker(input) {
        if (input.dataset.uiPickerBound === "true" || input.disabled || input.readOnly) {
            return null;
        }

        const mode = input.dataset.uiPicker === "month" ? "month" : "date";
        const wrapper = document.createElement("div");
        wrapper.className = "ui-picker";
        wrapper.dataset.pickerMode = mode;

        const trigger = document.createElement("button");
        trigger.type = "button";
        trigger.className = "ui-picker-trigger";
        trigger.setAttribute("aria-haspopup", "dialog");
        trigger.setAttribute("aria-expanded", "false");

        const triggerText = document.createElement("span");
        triggerText.className = "ui-picker-trigger-text";
        trigger.appendChild(triggerText);
        trigger.appendChild(createIcon());

        const popup = document.createElement("div");
        popup.className = "ui-picker-popup";
        popup.hidden = true;

        const header = document.createElement("div");
        header.className = "ui-picker-header";

        const prevYearBtn = document.createElement("button");
        prevYearBtn.type = "button";
        prevYearBtn.className = "ui-picker-nav";
        prevYearBtn.dataset.action = "prev-year";
        prevYearBtn.textContent = "«";

        const prevMonthBtn = document.createElement("button");
        prevMonthBtn.type = "button";
        prevMonthBtn.className = "ui-picker-nav";
        prevMonthBtn.dataset.action = "prev-month";
        prevMonthBtn.textContent = "‹";

        const currentBtn = document.createElement("button");
        currentBtn.type = "button";
        currentBtn.className = "ui-picker-current";
        currentBtn.dataset.action = "toggle-years";

        const nextMonthBtn = document.createElement("button");
        nextMonthBtn.type = "button";
        nextMonthBtn.className = "ui-picker-nav";
        nextMonthBtn.dataset.action = "next-month";
        nextMonthBtn.textContent = "›";

        const nextYearBtn = document.createElement("button");
        nextYearBtn.type = "button";
        nextYearBtn.className = "ui-picker-nav";
        nextYearBtn.dataset.action = "next-year";
        nextYearBtn.textContent = "»";

        header.appendChild(prevYearBtn);
        if (mode === "date") {
            header.appendChild(prevMonthBtn);
        }
        header.appendChild(currentBtn);
        if (mode === "date") {
            header.appendChild(nextMonthBtn);
        }
        header.appendChild(nextYearBtn);

        const yearsPanel = document.createElement("div");
        yearsPanel.className = "ui-picker-years";
        yearsPanel.hidden = true;

        const weekdays = document.createElement("div");
        weekdays.className = "ui-picker-weekdays";
        ["一", "二", "三", "四", "五", "六", "日"].forEach((label) => {
            const item = document.createElement("span");
            item.textContent = label;
            weekdays.appendChild(item);
        });

        const daysPanel = document.createElement("div");
        daysPanel.className = "ui-picker-days";

        const monthsPanel = document.createElement("div");
        monthsPanel.className = "ui-picker-months";

        const footer = document.createElement("div");
        footer.className = "ui-picker-footer";

        const clearBtn = document.createElement("button");
        clearBtn.type = "button";
        clearBtn.className = "ui-picker-footer-btn";
        clearBtn.dataset.action = "clear";
        clearBtn.textContent = "清除";

        const todayBtn = document.createElement("button");
        todayBtn.type = "button";
        todayBtn.className = "ui-picker-footer-btn";
        todayBtn.dataset.action = "today";
        todayBtn.textContent = mode === "month" ? "本月" : "今天";

        footer.appendChild(clearBtn);
        footer.appendChild(todayBtn);

        popup.appendChild(header);
        popup.appendChild(yearsPanel);
        if (mode === "date") {
            popup.appendChild(weekdays);
            popup.appendChild(daysPanel);
        } else {
            popup.appendChild(monthsPanel);
        }
        popup.appendChild(footer);

        input.classList.add("ui-picker-native");
        input.dataset.uiPickerBound = "true";
        input.parentNode.insertBefore(wrapper, input);
        wrapper.appendChild(input);
        wrapper.appendChild(trigger);
        wrapper.appendChild(popup);

        let selectedValue = parseDateValue(input.value, mode);
        let viewDate = selectedValue ? new Date(selectedValue) : new Date();
        viewDate.setDate(1);

        function syncTrigger() {
            triggerText.textContent = selectedValue ? formatDisplayValue(selectedValue, mode) : getPlaceholder(mode);
            trigger.classList.toggle("is-placeholder", !selectedValue);
            updateInvalidState(wrapper, input, trigger);
        }

        function dispatchNativeEvents() {
            input.dispatchEvent(new Event("input", { bubbles: true }));
            input.dispatchEvent(new Event("change", { bubbles: true }));
        }

        function applyValue(date, shouldDispatch = true) {
            selectedValue = date ? new Date(date) : null;
            input.value = selectedValue ? formatInputValue(selectedValue, mode) : "";
            syncTrigger();
            renderBody();
            if (shouldDispatch) {
                dispatchNativeEvents();
            }
        }

        function openPopup() {
            document.querySelectorAll(".ui-picker.is-open").forEach((openPicker) => {
                if (openPicker !== wrapper) {
                    openPicker.classList.remove("is-open");
                    const openPopupEl = openPicker.querySelector(".ui-picker-popup");
                    const openTrigger = openPicker.querySelector(".ui-picker-trigger");
                    const openYears = openPicker.querySelector(".ui-picker-years");
                    if (openPopupEl) openPopupEl.hidden = true;
                    if (openYears) openYears.hidden = true;
                    if (openTrigger) openTrigger.setAttribute("aria-expanded", "false");
                }
            });

            wrapper.classList.add("is-open");
            popup.hidden = false;
            trigger.setAttribute("aria-expanded", "true");
        }

        function closePopup() {
            wrapper.classList.remove("is-open");
            popup.hidden = true;
            yearsPanel.hidden = true;
            trigger.setAttribute("aria-expanded", "false");
        }

        function renderYearsPanel() {
            yearsPanel.innerHTML = "";
            const focusedYear = viewDate.getFullYear();
            for (let year = focusedYear - 10; year <= focusedYear + 10; year += 1) {
                const button = document.createElement("button");
                button.type = "button";
                button.className = "ui-picker-year";
                button.dataset.year = String(year);
                button.textContent = String(year);
                if (year === focusedYear) {
                    button.classList.add("is-active");
                }
                yearsPanel.appendChild(button);
            }
        }

        function renderDateGrid() {
            daysPanel.innerHTML = "";
            const year = viewDate.getFullYear();
            const month = viewDate.getMonth();
            const firstWeekday = (new Date(year, month, 1).getDay() + 6) % 7;
            const daysInMonth = new Date(year, month + 1, 0).getDate();
            const daysInPrevMonth = new Date(year, month, 0).getDate();

            for (let index = 0; index < firstWeekday; index += 1) {
                const muted = document.createElement("div");
                muted.className = "ui-picker-day";
                muted.textContent = String(daysInPrevMonth - firstWeekday + index + 1);
                daysPanel.appendChild(muted);
            }

            for (let day = 1; day <= daysInMonth; day += 1) {
                const button = document.createElement("button");
                button.type = "button";
                button.className = "ui-picker-day-btn";
                button.dataset.day = String(day);
                button.textContent = String(day);
                if (
                    selectedValue
                    && selectedValue.getFullYear() === year
                    && selectedValue.getMonth() === month
                    && selectedValue.getDate() === day
                ) {
                    button.classList.add("is-selected");
                }
                daysPanel.appendChild(button);
            }

            const trailingDays = 42 - (firstWeekday + daysInMonth);
            for (let day = 1; day <= trailingDays; day += 1) {
                const muted = document.createElement("div");
                muted.className = "ui-picker-day";
                muted.textContent = String(day);
                daysPanel.appendChild(muted);
            }
        }

        function renderMonthGrid() {
            monthsPanel.innerHTML = "";
            const year = viewDate.getFullYear();
            for (let month = 0; month < 12; month += 1) {
                const button = document.createElement("button");
                button.type = "button";
                button.className = "ui-picker-month-btn";
                button.dataset.month = String(month);
                button.textContent = `${pad(month + 1)}月`;
                if (
                    selectedValue
                    && selectedValue.getFullYear() === year
                    && selectedValue.getMonth() === month
                ) {
                    button.classList.add("is-selected");
                }
                monthsPanel.appendChild(button);
            }
        }

        function renderBody() {
            currentBtn.textContent = formatHeaderValue(viewDate, mode);
            if (!yearsPanel.hidden) {
                renderYearsPanel();
            }
            if (mode === "month") {
                renderMonthGrid();
            } else {
                renderDateGrid();
            }
        }

        trigger.addEventListener("click", () => {
            if (wrapper.classList.contains("is-open")) {
                closePopup();
                return;
            }
            openPopup();
            renderBody();
        });

        popup.addEventListener("click", (event) => {
            const actionTarget = event.target.closest("[data-action]");
            if (actionTarget) {
                const action = actionTarget.dataset.action;
                if (action === "toggle-years") {
                    yearsPanel.hidden = !yearsPanel.hidden;
                    if (!yearsPanel.hidden) {
                        renderYearsPanel();
                    }
                    return;
                }
                if (action === "prev-year") {
                    viewDate.setFullYear(viewDate.getFullYear() - 1);
                    renderBody();
                    return;
                }
                if (action === "next-year") {
                    viewDate.setFullYear(viewDate.getFullYear() + 1);
                    renderBody();
                    return;
                }
                if (action === "prev-month" && mode === "date") {
                    viewDate.setMonth(viewDate.getMonth() - 1);
                    renderBody();
                    return;
                }
                if (action === "next-month" && mode === "date") {
                    viewDate.setMonth(viewDate.getMonth() + 1);
                    renderBody();
                    return;
                }
                if (action === "clear") {
                    applyValue(null);
                    closePopup();
                    return;
                }
                if (action === "today") {
                    const now = new Date();
                    viewDate = new Date(now.getFullYear(), now.getMonth(), 1);
                    applyValue(mode === "month" ? new Date(now.getFullYear(), now.getMonth(), 1) : new Date(now.getFullYear(), now.getMonth(), now.getDate()));
                    closePopup();
                }
                return;
            }

            const yearButton = event.target.closest(".ui-picker-year");
            if (yearButton) {
                const year = Number(yearButton.dataset.year);
                if (!Number.isNaN(year)) {
                    viewDate.setFullYear(year);
                    yearsPanel.hidden = true;
                    renderBody();
                }
                return;
            }

            const dayButton = event.target.closest(".ui-picker-day-btn");
            if (dayButton && mode === "date") {
                const day = Number(dayButton.dataset.day);
                if (!Number.isNaN(day)) {
                    applyValue(new Date(viewDate.getFullYear(), viewDate.getMonth(), day));
                    closePopup();
                }
                return;
            }

            const monthButton = event.target.closest(".ui-picker-month-btn");
            if (monthButton && mode === "month") {
                const month = Number(monthButton.dataset.month);
                if (!Number.isNaN(month)) {
                    viewDate.setMonth(month);
                    applyValue(new Date(viewDate.getFullYear(), month, 1));
                    closePopup();
                }
            }
        });

        input.addEventListener("change", () => {
            input.dataset.uiPickerInteracted = "true";
            selectedValue = parseDateValue(input.value, mode);
            if (selectedValue) {
                viewDate = new Date(selectedValue.getFullYear(), selectedValue.getMonth(), 1);
            }
            syncTrigger();
            renderBody();
        });

        document.addEventListener("click", (event) => {
            if (!wrapper.contains(event.target)) {
                closePopup();
            }
        });

        document.addEventListener("keydown", (event) => {
            if (event.key === "Escape" && wrapper.classList.contains("is-open")) {
                closePopup();
            }
        });

        syncTrigger();
        renderBody();
        return { wrapper, input, trigger };
    }

    function initUiPickers() {
        document.querySelectorAll(PICKER_SELECTOR).forEach((input) => {
            buildPicker(input);
        });

        document.querySelectorAll("form").forEach((form) => {
            if (form.dataset.uiPickerValidationBound === "true") {
                return;
            }

            form.dataset.uiPickerValidationBound = "true";
            form.addEventListener("submit", (event) => {
                const formPickers = Array.from(form.querySelectorAll(".ui-picker"));
                const firstInvalid = formPickers.find((picker) => {
                    const input = picker.querySelector(".ui-picker-native");
                    const trigger = picker.querySelector(".ui-picker-trigger");
                    return input && trigger && !updateInvalidState(picker, input, trigger, true);
                });

                if (firstInvalid) {
                    event.preventDefault();
                    const input = firstInvalid.querySelector(".ui-picker-native");
                    const trigger = firstInvalid.querySelector(".ui-picker-trigger");
                    if (trigger) {
                        trigger.focus();
                    }
                    if (input && trigger) {
                        const labelText = resolveLabelText(input);
                        showValidationBubble(input, trigger, labelText ? `请选择${labelText}` : "请选择此项");
                    }
                }
            });
        });
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initUiPickers);
    } else {
        initUiPickers();
    }
})();

