(() => {
    "use strict";

    const ROLE_TO_DEPARTMENT = {
        SUPER_ADMIN: { code: "OFFICE", label: "综合办公室" },
        ADMIN: { code: "MANAGEMENT", label: "管理部" },
        ENGINEER: { code: "ENGINEERING", label: "工程部" },
        ACCOUNTANT: { code: "FINANCE", label: "财务部" },
        RESIDENT: { code: "", label: "无部门" }
    };

    function syncUserDepartment(modal) {
        const roleInput = modal.querySelector("[data-user-create-role]");
        const departmentInput = modal.querySelector("[data-user-create-department]");
        const departmentLabelInput = modal.querySelector("[data-user-create-department-label]");
        if (!roleInput || !departmentInput || !departmentLabelInput) {
            return;
        }
        const next = ROLE_TO_DEPARTMENT[roleInput.value] || ROLE_TO_DEPARTMENT.ADMIN;
        departmentInput.value = next.code;
        departmentLabelInput.value = next.label;
    }

    function isAnyModalOpen(modals) {
        return modals.some((modal) => !modal.hidden);
    }

    function formatNumberLabel(value) {
        const normalized = String(value || "").replace(/^0+/, "");
        return normalized || String(value || "");
    }

    function parseUnitNo(unitNo) {
        const source = String(unitNo || "").trim();
        const digits = source.replace(/\D/g, "");
        if (digits.length >= 6) {
            const buildingNo = formatNumberLabel(digits.slice(0, 2));
            const unitNoPart = formatNumberLabel(digits.slice(2, 3));
            const floorNo = formatNumberLabel(digits.slice(3, 5));
            const roomNo = formatNumberLabel(digits.slice(5));
            return {
                buildingKey: buildingNo,
                buildingLabel: `${buildingNo}栋`,
                unitKey: unitNoPart,
                unitLabel: `${unitNoPart}单元`,
                roomLabel: `${floorNo}层${roomNo}户`,
                displayLabel: source
            };
        }

        return {
            buildingKey: "其他",
            buildingLabel: "其他",
            unitKey: "全部",
            unitLabel: "全部",
            roomLabel: source || "未命名房屋",
            displayLabel: source
        };
    }

    function uniqueBy(items, keySelector) {
        const map = new Map();
        items.forEach((item) => {
            const key = keySelector(item);
            if (!map.has(key)) {
                map.set(key, item);
            }
        });
        return Array.from(map.values());
    }

    function sortByNumberLabel(a, b, field) {
        const aNumber = Number.parseInt(a[field], 10);
        const bNumber = Number.parseInt(b[field], 10);
        if (Number.isFinite(aNumber) && Number.isFinite(bNumber)) {
            return aNumber - bNumber;
        }
        return String(a[field]).localeCompare(String(b[field]), "zh-CN");
    }

    function initUnitPickers(scope) {
        const pickers = Array.from(scope.querySelectorAll("[data-unit-picker]"));
        pickers.forEach((picker) => {
            const valueInput = picker.querySelector("[data-unit-picker-value]");
            const displayInput = picker.querySelector("[data-unit-picker-input]");
            const trigger = picker.querySelector("[data-unit-picker-trigger]");
            const panel = picker.querySelector("[data-unit-picker-panel]");
            const buildingList = picker.querySelector("[data-unit-picker-buildings]");
            const unitList = picker.querySelector("[data-unit-picker-units]");
            const roomList = picker.querySelector("[data-unit-picker-rooms]");
            const optionNodes = Array.from(picker.querySelectorAll("[data-unit-picker-option]"));
            if (!valueInput || !displayInput || !panel || !buildingList || !unitList || !roomList) {
                return;
            }

            const items = optionNodes
                .map((node) => {
                    const unitNo = node.dataset.unitNo || "";
                    return {
                        id: node.dataset.id || "",
                        unitNo,
                        ...parseUnitNo(unitNo)
                    };
                })
                .filter((item) => item.id && item.unitNo);

            let selectedBuildingKey = null;
            let selectedUnitKey = null;

            function selectedItem() {
                return items.find((item) => item.id === valueInput.value);
            }

            function syncDisplay() {
                const current = selectedItem();
                displayInput.value = current ? current.displayLabel : "";
                if (current) {
                    selectedBuildingKey = current.buildingKey;
                    selectedUnitKey = current.unitKey;
                }
            }

            function makeOptionButton(label, active, onClick) {
                const button = document.createElement("button");
                button.type = "button";
                button.className = `unit-picker-option${active ? " is-active" : ""}`;
                button.textContent = label;
                button.addEventListener("click", onClick);
                return button;
            }

            function renderBuildings() {
                buildingList.replaceChildren();
                const buildings = uniqueBy(items, (item) => item.buildingKey)
                    .sort((a, b) => sortByNumberLabel(a, b, "buildingKey"));
                buildings.forEach((item) => {
                    buildingList.appendChild(makeOptionButton(
                        item.buildingLabel,
                        selectedBuildingKey === item.buildingKey,
                        () => {
                            selectedBuildingKey = item.buildingKey;
                            selectedUnitKey = null;
                            renderAll();
                        }
                    ));
                });
            }

            function renderUnits() {
                unitList.replaceChildren();
                const units = uniqueBy(
                    items.filter((item) => item.buildingKey === selectedBuildingKey),
                    (item) => item.unitKey
                ).sort((a, b) => sortByNumberLabel(a, b, "unitKey"));
                units.forEach((item) => {
                    unitList.appendChild(makeOptionButton(
                        item.unitLabel,
                        selectedUnitKey === item.unitKey,
                        () => {
                            selectedUnitKey = item.unitKey;
                            renderAll();
                        }
                    ));
                });
            }

            function renderRooms() {
                roomList.replaceChildren();
                if (!selectedBuildingKey) {
                    const empty = document.createElement("div");
                    empty.className = "unit-picker-empty";
                    empty.textContent = "请选择楼栋";
                    roomList.appendChild(empty);
                    return;
                }

                const rooms = items
                    .filter((item) => item.buildingKey === selectedBuildingKey
                        && (!selectedUnitKey || item.unitKey === selectedUnitKey))
                    .sort((a, b) => String(a.unitNo).localeCompare(String(b.unitNo), "zh-CN", { numeric: true }));
                if (rooms.length === 0) {
                    const empty = document.createElement("div");
                    empty.className = "unit-picker-empty";
                    empty.textContent = "暂无房屋";
                    roomList.appendChild(empty);
                    return;
                }

                rooms.forEach((item) => {
                    roomList.appendChild(makeOptionButton(
                        `${item.roomLabel}　${item.unitNo}`,
                        valueInput.value === item.id,
                        () => {
                            valueInput.value = item.id;
                            displayInput.value = item.displayLabel;
                            panel.hidden = true;
                            valueInput.dispatchEvent(new Event("change", { bubbles: true }));
                        }
                    ));
                });
            }

            function renderAll() {
                renderBuildings();
                renderUnits();
                renderRooms();
            }

            function openPanel() {
                if (!selectedBuildingKey && items.length > 0) {
                    const current = selectedItem() || items[0];
                    selectedBuildingKey = current.buildingKey;
                    selectedUnitKey = current.unitKey;
                }
                renderAll();
                panel.hidden = false;
            }

            displayInput.addEventListener("click", openPanel);
            trigger?.addEventListener("click", openPanel);
            picker.addEventListener("click", (event) => {
                event.stopPropagation();
            });

            document.addEventListener("click", (event) => {
                if (!picker.contains(event.target)) {
                    panel.hidden = true;
                }
            });

            syncDisplay();
        });
    }

    function initManagementCreateModals() {
        const modals = Array.from(document.querySelectorAll("[data-management-create-modal]"));
        if (modals.length === 0) {
            return;
        }

        const closeModal = (modal) => {
            modal.hidden = true;
            modal.setAttribute("aria-hidden", "true");
            if (!isAnyModalOpen(modals)) {
                document.body.classList.remove("management-create-modal-open");
            }
        };

        const openModal = (modal) => {
            if (modal.querySelector("[data-user-create-role]")) {
                syncUserDepartment(modal);
            }
            modal.hidden = false;
            modal.setAttribute("aria-hidden", "false");
            document.body.classList.add("management-create-modal-open");

            const focusTarget = modal.querySelector("[data-management-create-autofocus]");
            window.requestAnimationFrame(() => {
                focusTarget?.focus();
            });
        };

        modals.forEach((modal) => {
            const modalName = modal.getAttribute("data-management-create-modal");
            const openButtons = document.querySelectorAll(`[data-management-create-open="${modalName}"]`);
            const closeButtons = modal.querySelectorAll("[data-management-create-close]");
            const roleInput = modal.querySelector("[data-user-create-role]");
            const form = modal.querySelector("form");

            initUnitPickers(modal);

            openButtons.forEach((button) => {
                button.addEventListener("click", () => openModal(modal));
            });

            closeButtons.forEach((button) => {
                button.addEventListener("click", () => closeModal(modal));
            });

            modal.addEventListener("click", (event) => {
                if (event.target === modal) {
                    closeModal(modal);
                }
            });

            roleInput?.addEventListener("change", () => syncUserDepartment(modal));
            form?.addEventListener("reset", () => {
                window.requestAnimationFrame(() => syncUserDepartment(modal));
            });

            if (modal.dataset.openOnLoad === "true") {
                openModal(modal);
            }
        });

        document.addEventListener("keydown", (event) => {
            if (event.key !== "Escape") {
                return;
            }
            [...modals].reverse().find((modal) => {
                if (modal.hidden) {
                    return false;
                }
                closeModal(modal);
                return true;
            });
        });
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initManagementCreateModals, { once: true });
    } else {
        initManagementCreateModals();
    }
})();
