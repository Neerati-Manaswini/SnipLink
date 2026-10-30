"use strict";

const $ = sel => document.querySelector(sel);

// ---- Helpers --------------------------------------------------------------------
const store = {
    get(key, fallback) {
        try {
            const raw = localStorage.getItem(key);
            return raw === null ? fallback : JSON.parse(raw);
        } catch (e) { return fallback; }
    },
    set(key, value) {
        try { localStorage.setItem(key, JSON.stringify(value)); } catch (e) { /* storage unavailable */ }
    }
};

function el(tag, className, text) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (text !== undefined) node.textContent = text;
    return node;
}

const stripProtocol = url => url.replace(/^https?:\/\//, "");

function safeHref(url) {
    try {
        const u = new URL(url);
        return (u.protocol === "http:" || u.protocol === "https:") ? u.href : null;
    } catch (e) { return null; }
}

function fmtDate(iso) {
    return new Date(iso).toLocaleString("en-US", { day: "numeric", month: "short", hour: "numeric", minute: "2-digit" });
}

function expiryText(link) {
    if (!link.expiresAt) return "Never expires";
    if (link.expired) return "Expired";
    const ms = new Date(link.expiresAt) - Date.now();
    const hours = Math.ceil(ms / 3600000);
    if (hours < 48) return "Expires in " + Math.max(hours, 1) + (hours === 1 ? " hour" : " hours");
    return "Expires in " + Math.ceil(hours / 24) + " days";
}

async function api(path, options) {
    const res = await fetch(path, options);
    if (res.status === 204) return null;
    const data = await res.json().catch(() => ({}));
    if (!res.ok) throw new Error(data.error || "Something went wrong. Please try again.");
    return data;
}

async function copyText(text) {
    try {
        await navigator.clipboard.writeText(text);
        return true;
    } catch (e) {
        const area = document.createElement("textarea");
        area.value = text;
        document.body.appendChild(area);
        area.select();
        let ok = false;
        try { ok = document.execCommand("copy"); } catch (err) { ok = false; }
        area.remove();
        return ok;
    }
}

function flashLabel(button, text) {
    const original = button.dataset.label || button.textContent;
    button.dataset.label = original;
    button.textContent = text;
    setTimeout(() => { button.textContent = original; }, 1400);
}

// ---- State ----------------------------------------------------------------------
// The browser remembers which links it created: [{code, token}]. The token allows deleting.
const saved = store.get("mine", []);
let mine = Array.isArray(saved) ? saved.filter(x => x && typeof x.code === "string") : [];
let lastShortUrl = "";

// ---- Rendering ------------------------------------------------------------------
function renderLinks(links) {
    const list = $("#links");
    list.replaceChildren();
    $("#empty").hidden = links.length > 0;

    links.forEach(link => {
        const li = el("li", "link");

        const short = el("a", "short", stripProtocol(link.shortUrl));
        short.href = link.shortUrl;
        short.target = "_blank";
        short.rel = "noopener";

        const orig = el("span", "orig", link.originalUrl);
        orig.title = link.originalUrl;

        const meta = el("div", "meta");
        const clicks = el("span");
        clicks.append(el("span", "clicks", String(link.clicks)), document.createTextNode(link.clicks === 1 ? " click" : " clicks"));
        meta.append(clicks, el("span", null, "Created " + fmtDate(link.createdAt)));
        meta.append(el("span", link.expired ? "badge-expired" : null, expiryText(link)));
        if (link.lastClickedAt) meta.append(el("span", null, "Last click " + fmtDate(link.lastClickedAt)));

        const actions = el("div", "actions");
        const copy = el("button", "btn ghost small", "Copy");
        copy.type = "button";
        copy.addEventListener("click", async () => flashLabel(copy, (await copyText(link.shortUrl)) ? "Copied" : "Press Ctrl+C"));
        const del = el("button", "btn danger small", "Delete");
        del.type = "button";
        del.addEventListener("click", () => removeLink(link.code));
        actions.append(copy, del);

        li.append(short, actions, orig, meta);
        list.appendChild(li);
    });

    $("#stat-links").textContent = String(links.length);
    $("#stat-clicks").textContent = String(links.reduce((sum, l) => sum + l.clicks, 0));
}

function showBanner(message) {
    const banner = $("#banner");
    banner.textContent = message;
    banner.hidden = false;
}

function showFormError(message) {
    const box = $("#form-error");
    box.textContent = message || "";
    box.hidden = !message;
}

// ---- Actions --------------------------------------------------------------------
async function refreshList() {
    if (mine.length === 0) {
        renderLinks([]);
        return;
    }
    try {
        const codes = mine.map(m => m.code).join(",");
        const links = await api("/api/links?codes=" + encodeURIComponent(codes));
        // forget links that no longer exist on the server
        const existing = new Set(links.map(l => l.code));
        const pruned = mine.filter(m => existing.has(m.code));
        if (pruned.length !== mine.length) {
            mine = pruned;
            store.set("mine", mine);
        }
        renderLinks(links);
    } catch (err) {
        showBanner(err.message);
    }
}

async function removeLink(code) {
    if (!confirm("Delete this short link? It will stop working immediately.")) return;
    const entry = mine.find(m => m.code === code);
    try {
        await api("/api/links/" + encodeURIComponent(code), {
            method: "DELETE",
            headers: { "X-Delete-Token": entry ? entry.token : "" }
        });
        mine = mine.filter(m => m.code !== code);
        store.set("mine", mine);
        if (lastShortUrl.endsWith("/" + code)) $("#result").hidden = true;
        await refreshList();
    } catch (err) {
        showBanner(err.message);
    }
}

$("#create-form").addEventListener("submit", async e => {
    e.preventDefault();
    showFormError("");
    $("#banner").hidden = true;

    const url = $("#url").value.trim();
    if (!url) {
        showFormError("Enter a URL to shorten.");
        return;
    }
    const expiry = $("#expiry").value;
    const body = { url, alias: $("#alias").value.trim(), expiresInDays: expiry ? Number(expiry) : null };

    const submit = $("#submit");
    submit.disabled = true;
    try {
        const link = await api("/api/links", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(body)
        });
        mine.unshift({ code: link.code, token: link.deleteToken });
        store.set("mine", mine);

        lastShortUrl = link.shortUrl;
        const anchor = $("#result-link");
        anchor.textContent = link.shortUrl;
        anchor.href = link.shortUrl;
        $("#result").hidden = false;

        $("#url").value = "";
        $("#alias").value = "";
        await refreshList();
    } catch (err) {
        showFormError(err.message);
    } finally {
        submit.disabled = false;
    }
});

$("#result-copy").addEventListener("click", async () => {
    flashLabel($("#result-copy"), (await copyText(lastShortUrl)) ? "Copied" : "Press Ctrl+C");
});

$("#refresh").addEventListener("click", refreshList);

document.addEventListener("visibilitychange", () => {
    if (document.visibilityState === "visible") refreshList();
});
setInterval(() => { if (document.visibilityState === "visible") refreshList(); }, 15000);

// ---- Start ----------------------------------------------------------------------
$("#host-prefix").textContent = location.host + "/";

const reason = new URLSearchParams(location.search).get("error");
if (reason === "notfound") showBanner("That short link doesn't exist. It may have been deleted or mistyped.");
if (reason === "expired") showBanner("That short link has expired.");
if (reason) history.replaceState(null, "", location.pathname);

refreshList();
