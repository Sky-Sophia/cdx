(() => {
    "use strict";

    const PICKER_SELECTOR = "[data-resident-date-picker]";

    function pad(value) {
        return String(value).padStart(2, "0");
    }

    function parseIsoDate(value) {
        if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) {
            return null;
        }

        const [year, month, day] = value.split("-").map(Number);
        const date = new Date(year, month - 1, day);
        if (
            Number.isNaN(date.getTime())
            || date.getFullYear() !== year
            || date.getMonth() !== month - 1
            || date.getDate() !== day
        ) {
            return null;
        }

        return date;
    }

    function formatHiddenValue(date) {
        return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
    }

    function formatDisplayValue(date) {
        return `${date.getFullYear()}/${pad(date.getMonth() + 1)}/${pad(date.getDate())}`;
    }

    function formatCurrentMonth(date) {
        return `${date.getFullYear()}年${pad(date.getMonth() + 1)}月`;
    }

    function createMutedDay(day) {
        const element = document.createElement("div");
        element.className = "resident-date-picker-day";
        element.textContent = String(day);
        return element;
    }

    function setupPicker(root) {
        const hiddenInput = root.querySelector("[data-date-picker-value]");
        const displayInput = root.querySelector("[data-date-picker-display]");
        const popup = root.querySelector("[data-date-picker-popup]");
        const currentMonth = root.querySelector("[data-date-picker-action='toggle-years']");
        const yearsPanel = root.querySelector("[data-date-picker-years]");
        const daysPanel = root.querySelector("[data-date-picker-days]");
        const toggle = root.querySelector("[data-date-picker-toggle]");

        if (!hiddenInput || !displayInput || !popup || !currentMonth || !yearsPanel || !daysPanel || !toggle) {
            return;
        }

        let selectedDate = parseIsoDate(hiddenInput.value);
        let currentDate = selectedDate ? new Date(selectedDate) : new Date();

        function syncInputs() {
            hiddenInput.value = selectedDate ? formatHiddenValue(selectedDate) : "";
            displayInput.value = selectedDate ? formatDisplayValue(selectedDate) : "";
            hiddenInput.dispatchEvent(new Event("change", { bubbles: true }));
        }

        function closeYearsPanel() {
            yearsPanel.hidden = true;
        }

        function openPopup() {
            document.querySelectorAll(`${PICKER_SELECTOR}.is-open`).forEach((picker) => {
                if (picker !== root) {
                    picker.classList.remove("is-open");
                    const otherPopup = picker.querySelector("[data-date-picker-popup]");
                    const otherDisplay = picker.querySelector("[data-date-picker-display]");
                    const otherYears = picker.querySelector("[data-date-picker-years]");
                    if (otherPopup) {
                        otherPopup.hidden = true;
                    }
                    if (otherDisplay) {
                        otherDisplay.setAttribute("aria-expanded", "false");
                    }
                    if (otherYears) {
                        otherYears.hidden = true;
                    }
                }
            });

            root.classList.add("is-open");
            popup.hidden = false;
            displayInput.setAttribute("aria-expanded", "true");
        }

        function closePopup() {
            root.classList.remove("is-open");
            popup.hidden = true;
            closeYearsPanel();
            displayInput.setAttribute("aria-expanded", "false");
        }

        function renderYearsPanel() {
            yearsPanel.innerHTML = "";
            const focusedYear = currentDate.getFullYear();
            const startYear = focusedYear - 10;
            const endYear = focusedYear + 10;

            for (let year = startYear; year <= endYear; year += 1) {
                const button = document.createElement("button");
                button.type = "button";
                button.className = "resident-date-picker-year";
                button.textContent = String(year);
                button.dataset.year = String(year);
                if (year === focusedYear) {
                    button.classList.add("is-active");
                }
                yearsPanel.appendChild(button);
            }
        }

        function renderCalendar() {
            const year = currentDate.getFullYear();
            const month = currentDate.getMonth();
            const firstWeekday = (new Date(year, month, 1).getDay() + 6) % 7;
            const daysInMonth = new Date(year, month + 1, 0).getDate();
            const daysInPrevMonth = new Date(year, month, 0).getDate();

            currentMonth.textContent = formatCurrentMonth(currentDate);
            daysPanel.innerHTML = "";

            for (let index = 0; index < firstWeekday; index += 1) {
                const day = daysInPrevMonth - firstWeekday + index + 1;
                daysPanel.appendChild(createMutedDay(day));
            }

            for (let day = 1; day <= daysInMonth; day += 1) {
                const button = document.createElement("button");
                button.type = "button";
                button.className = "resident-date-picker-day-btn";
                button.textContent = String(day);
                button.dataset.day = String(day);

                if (
                    selectedDate
                    && selectedDate.getFullYear() === year
                    && selectedDate.getMonth() === month
                    && selectedDate.getDate() === day
                ) {
                    button.classList.add("is-selected");
                }

                daysPanel.appendChild(button);
            }

            const totalCells = firstWeekday + daysInMonth;
            const trailingDays = 42 - totalCells;
            for (let day = 1; day <= trailingDays; day += 1) {
                daysPanel.appendChild(createMutedDay(day));
            }

            if (!yearsPanel.hidden) {
                renderYearsPanel();
            }
        }

        function selectDate(date) {
            selectedDate = new Date(date.getFullYear(), date.getMonth(), date.getDate());
            currentDate = new Date(selectedDate);
            syncInputs();
            renderCalendar();
        }

        syncInputs();
        renderCalendar();

        toggle.addEventListener("click", () => {
            if (root.classList.contains("is-open")) {
                closePopup();
                return;
            }
            openPopup();
            renderCalendar();
        });

        root.addEventListener("click", (event) => {
            const actionTarget = event.target.closest("[data-date-picker-action]");
            if (actionTarget) {
                const action = actionTarget.dataset.datePickerAction;
                if (action === "toggle-years") {
                    yearsPanel.hidden = !yearsPanel.hidden;
                    if (!yearsPanel.hidden) {
                        renderYearsPanel();
                    }
                    return;
                }

                if (action === "prev-month") {
                    currentDate.setMonth(currentDate.getMonth() - 1);
                    renderCalendar();
                    return;
                }

                if (action === "next-month") {
                    currentDate.setMonth(currentDate.getMonth() + 1);
                    renderCalendar();
                    return;
                }

                if (action === "prev-year") {
                    currentDate.setFullYear(currentDate.getFullYear() - 1);
                    renderCalendar();
                    return;
                }

                if (action === "next-year") {
                    currentDate.setFullYear(currentDate.getFullYear() + 1);
                    renderCalendar();
                    return;
                }

                if (action === "clear") {
                    selectedDate = null;
                    syncInputs();
                    renderCalendar();
                    return;
                }

                if (action === "today") {
                    const today = new Date();
                    selectDate(today);
                    closePopup();
                }
                return;
            }

            const dayButton = event.target.closest(".resident-date-picker-day-btn");
            if (dayButton) {
                const day = Number(dayButton.dataset.day);
                if (!Number.isNaN(day)) {
                    selectDate(new Date(currentDate.getFullYear(), currentDate.getMonth(), day));
                    closePopup();
                }
                return;
            }

            const yearButton = event.target.closest(".resident-date-picker-year");
            if (yearButton) {
                const year = Number(yearButton.dataset.year);
                if (!Number.isNaN(year)) {
                    currentDate.setFullYear(year);
                    closeYearsPanel();
                    renderCalendar();
                }
            }
        });

        document.addEventListener("click", (event) => {
            if (!root.contains(event.target)) {
                closePopup();
            }
        });

        document.addEventListener("keydown", (event) => {
            if (event.key === "Escape" && root.classList.contains("is-open")) {
                closePopup();
            }
        });
    }

    function init() {
        document.querySelectorAll(PICKER_SELECTOR).forEach(setupPicker);
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
})();

