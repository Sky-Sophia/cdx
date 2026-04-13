(() => {
    "use strict";

    const SCROLLBAR_CLASS = "page-overlay-scrollbar";
    const ACTIVE_CLASS = "is-active";
    const DRAGGING_CLASS = "is-dragging";
    const HOVER_EDGE_WIDTH = 36;

    let scrollbar;
    let thumb;
    let hideTimer = 0;
    let dragging = false;
    let dragStartY = 0;
    let dragStartScrollTop = 0;

    function getDocumentHeight() {
        const doc = document.documentElement;
        const body = document.body;
        return Math.max(
            doc.scrollHeight,
            body ? body.scrollHeight : 0,
            doc.offsetHeight,
            body ? body.offsetHeight : 0
        );
    }

    function getViewportHeight() {
        return window.innerHeight || document.documentElement.clientHeight || 0;
    }

    function getMaxScrollTop() {
        return Math.max(getDocumentHeight() - getViewportHeight(), 0);
    }

    function isScrollLocked() {
        const root = document.documentElement;
        const body = document.body;
        const hasModalOpenClass = (element) => element && Array.from(element.classList).some((className) => className.endsWith("-modal-open"));
        return hasModalOpenClass(root) || hasModalOpenClass(body);
    }

    function isScrollable() {
        return getMaxScrollTop() > 1 && !isScrollLocked();
    }

    function hide() {
        if (!scrollbar || dragging) {
            return;
        }
        scrollbar.classList.remove(ACTIVE_CLASS);
    }

    function scheduleHide(delay = 300) {
        if (hideTimer) {
            window.clearTimeout(hideTimer);
            hideTimer = 0;
        }
        if (!dragging) {
            hideTimer = window.setTimeout(hide, delay);
        }
    }

    function show(autoHide = true) {
        if (!scrollbar || !isScrollable()) {
            hide();
            return;
        }

        scrollbar.classList.add(ACTIVE_CLASS);

        if (autoHide && !dragging) {
            scheduleHide(900);
        }
    }

    function updateThumb() {
        if (!scrollbar || !thumb) {
            return;
        }

        if (!isScrollable()) {
            hide();
            return;
        }

        const trackHeight = scrollbar.clientHeight;
        const viewportHeight = getViewportHeight();
        const documentHeight = getDocumentHeight();
        const maxScrollTop = getMaxScrollTop();
        const scrollTop = window.scrollY || document.documentElement.scrollTop || 0;
        const thumbHeight = Math.max(Math.round(trackHeight * viewportHeight / documentHeight), 36);
        const maxThumbTop = Math.max(trackHeight - thumbHeight, 0);
        const thumbTop = maxScrollTop > 0 ? Math.round(maxThumbTop * scrollTop / maxScrollTop) : 0;

        thumb.style.height = `${thumbHeight}px`;
        thumb.style.transform = `translateY(${thumbTop}px)`;
    }

    function handleScroll() {
        updateThumb();
        show(true);
    }

    function handleResize() {
        updateThumb();
        if (!isScrollable()) {
            hide();
        }
    }

    function handleMouseMove(event) {
        if (dragging) {
            return;
        }
        if (window.innerWidth - event.clientX <= HOVER_EDGE_WIDTH) {
            updateThumb();
            show(false);
            return;
        }
        if (scrollbar && scrollbar.classList.contains(ACTIVE_CLASS) && !scrollbar.matches(":hover")) {
            scheduleHide(250);
        }
    }

    function handleDragMove(event) {
        if (!dragging || !scrollbar) {
            return;
        }

        event.preventDefault();
        const trackHeight = scrollbar.clientHeight;
        const thumbHeight = thumb.offsetHeight;
        const maxThumbTop = Math.max(trackHeight - thumbHeight, 1);
        const maxScrollTop = getMaxScrollTop();
        const deltaY = event.clientY - dragStartY;
        const scrollDelta = deltaY / maxThumbTop * maxScrollTop;

        window.scrollTo(0, dragStartScrollTop + scrollDelta);
        updateThumb();
    }

    function stopDragging() {
        if (!dragging) {
            return;
        }
        dragging = false;
        scrollbar.classList.remove(DRAGGING_CLASS);
        document.body.style.removeProperty("user-select");
        show(true);
    }

    function startDragging(event) {
        if (!isScrollable()) {
            return;
        }

        event.preventDefault();
        dragging = true;
        dragStartY = event.clientY;
        dragStartScrollTop = window.scrollY || document.documentElement.scrollTop || 0;
        scrollbar.classList.add(DRAGGING_CLASS);
        document.body.style.userSelect = "none";
        show(false);
    }

    function createScrollbar() {
        scrollbar = document.createElement("div");
        scrollbar.className = SCROLLBAR_CLASS;
        scrollbar.setAttribute("aria-hidden", "true");

        thumb = document.createElement("div");
        thumb.className = `${SCROLLBAR_CLASS}-thumb`;
        scrollbar.appendChild(thumb);
        document.body.appendChild(scrollbar);

        thumb.addEventListener("mousedown", startDragging);
        scrollbar.addEventListener("mouseenter", () => show(false));
        scrollbar.addEventListener("mouseleave", () => show(true));
    }

    function init() {
        if (!document.body || document.querySelector(`.${SCROLLBAR_CLASS}`)) {
            return;
        }

        createScrollbar();
        updateThumb();

        window.addEventListener("scroll", handleScroll, { passive: true });
        window.addEventListener("resize", handleResize, { passive: true });
        document.addEventListener("mousemove", handleMouseMove, { passive: true });
        document.addEventListener("mousemove", handleDragMove);
        document.addEventListener("mouseup", stopDragging);
        window.addEventListener("load", handleResize);

        const contentObserver = new MutationObserver(handleResize);
        contentObserver.observe(document.body, {
            childList: true,
            subtree: true
        });

        const classObserver = new MutationObserver(handleResize);
        classObserver.observe(document.documentElement, {
            attributes: true,
            attributeFilter: ["class"]
        });
        classObserver.observe(document.body, {
            attributes: true,
            attributeFilter: ["class"]
        });
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init, { once: true });
    } else {
        init();
    }
})();
