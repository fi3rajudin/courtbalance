const card = document.querySelector('#publicCard'),
    modal = document.querySelector('#qrModal'),
    img = document.querySelector('#qrImage');
const token = new URLSearchParams(location.search).get('token');
let state;
async function api(url, opt = {}) {
    const r = await fetch(url, opt);
    if (!r.ok) {
        let b = {};
        try {
            b = await r.json()
        } catch {}
        throw new Error(b.message || 'Request failed')
    }
    return r.json()
}
async function load() {
    if (!token) {
        card.innerHTML = '<h2>Invalid session link</h2>';
        return
    }
    try {
        state = await api(`/api/public/sessions/${encodeURIComponent(token)}`);
        render()
    } catch (e) {
        card.innerHTML = `<h2>Unable to load session</h2><p>${esc(e.message)}</p>`
    }
}

function render() {
    card.innerHTML = `<h1>${esc(state.title)}</h1><p>${formatDate(state.sessionDate)} · ${state.status}</p>${state.notes ? `<p>${esc(state.notes)}</p>` : ''}<div class="grid"><div class="metric"><b>RM${money(state.totalCost)}</b><br>Total</div><div class="metric"><b>${state.participantCount}</b><br>Players</div></div><h2>Expenses</h2>${state.expenses.map(e => `<div class="list-item"><b>${esc(e.type)}</b> ${esc(e.description || '')}<br>RM${money(e.amount)} · paid/provided by ${esc(e.paidBy)}</div>`).join('') || '<p>No expenses.</p>'}<h2>Payments</h2>${state.transfers.map(transferHtml).join('') || '<p>Everyone is already settled.</p>'}`;
    document.querySelectorAll('[data-paid]').forEach(b => b.onclick = () => markPaid(b.dataset.paid));
    document.querySelectorAll('[data-undo]').forEach(b => b.onclick = () => undo(b.dataset.undo));
    document.querySelectorAll('[data-qr]').forEach(b => b.onclick = () => showQr(b.dataset.qr, b.dataset.name, b.dataset.amount));
    startCountdowns()
}

function transferHtml(t) {
    const paid = t.status === 'PAID';
    return `<div class="transfer ${paid ? 'paid' : ''}"><b>${esc(t.payer)} → ${esc(t.payee)}</b><div class="amount">RM${money(t.amount)}</div><button data-qr="${t.payeeMemberId}" data-name="${esc(t.payee)}" data-amount="${money(t.amount)}" class="secondary">Show ${esc(t.payee)}'s QR</button>${paid ? `<p>PAID</p>${canUndo(t) ? `<button data-undo="${t.id}" class="ghost">Undo <span data-countdown="${t.undoUntil}"></span></button>` : ''}` : `<button data-paid="${t.id}">I've paid</button>`}</div>`
}
async function markPaid(id) {
    try {
        await api(`/api/public/sessions/${encodeURIComponent(token)}/transfers/${id}/paid`, {
            method: 'POST'
        });
        await load()
    } catch (e) {
        alert(e.message)
    }
}
async function undo(id) {
    try {
        await api(`/api/public/sessions/${encodeURIComponent(token)}/transfers/${id}/undo`, {
            method: 'POST'
        });
        await load()
    } catch (e) {
        alert(e.message)
    }
}

function showQr(id, name, amount) {
    document.querySelector('#qrTitle').textContent = `Pay ${name}`;
    document.querySelector('#qrAmount').textContent = `RM${amount}`;
    img.src = `/api/public/sessions/${encodeURIComponent(token)}/payees/${id}/qr?t=${Date.now()}`;
    modal.classList.remove('hidden')
}
document.querySelector('#closeQr').onclick = () => {
    modal.classList.add('hidden');
    img.src = ''
};

function canUndo(t) {
    return t.undoUntil && Date.now() <= new Date(t.undoUntil).getTime()
}

function startCountdowns() {
    const els = [...document.querySelectorAll('[data-countdown]')];
    if (!els.length) return;
    const tick = () => {
        els.forEach(el => {
            const ms = new Date(el.dataset.countdown) - Date.now();
            if (ms <= 0) {
                el.closest('button')?.remove();
                return
            }
            const s = Math.ceil(ms / 1000);
            el.textContent = `(${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')})`
        })
    };
    tick();
    setInterval(tick, 1000)
}

function formatDate(dateString) {
    if (!dateString) {
        return '';
    }

    const [year, month, day] = dateString.split('-');

    return `${day}-${month}-${year}`;
}

function money(v) {
    return Number(v).toFixed(2)
}

function esc(s) {
    return String(s ?? '').replace(/[&<>'"]/g, c => ({
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        "'": '&#39;',
        '"': '&quot;'
    } [c]))
}
load();