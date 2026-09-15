const $ = s => document.querySelector(s);

let csrf = null;
let members = [];
let currentUser = null;
let maintainers = [];


function csrfHeader() {
    const c = document.cookie
        .split('; ')
        .find(x => x.startsWith('XSRF-TOKEN='));

    return c
        ? { 'X-XSRF-TOKEN': decodeURIComponent(c.split('=')[1]) }
        : {};
}


async function api(url, opt = {}) {
    opt.headers = {
        ...(opt.body instanceof FormData
            ? {}
            : { 'Content-Type': 'application/json' }),
        ...csrfHeader(),
        ...(opt.headers || {})
    };

    const r = await fetch(url, opt);

    if (!r.ok) {
        let b = {};

        try {
            b = await r.json();
        } catch {
        }

        const e = new Error(
            b.message || `Request failed (${r.status})`
        );

        e.status = r.status;
        throw e;
    }

    return r.status === 204
        ? null
        : r.json();
}


async function boot() {
    try {
        currentUser = await api('/api/auth/me');

        await api('/api/auth/csrf');

        showApp();
        configureRoleUi();

        await refresh();

    } catch {
    }
}


$('#loginForm').onsubmit = async e => {
    e.preventDefault();

    try {
        await api('/api/auth/login', {
            method: 'POST',
            body: JSON.stringify({
                username: $('#username').value,
                password: $('#password').value
            })
        });

        currentUser = await api('/api/auth/me');

        await api('/api/auth/csrf');

        showApp();
        configureRoleUi();

        await refresh();

    } catch (err) {
        $('#loginMsg').textContent = err.message;
    }
};

function configureRoleUi() {
    const maintainerButton =
        document.querySelector('button[data-tab="maintainers"]');

    if (!maintainerButton) {
        return;
    }

    maintainerButton.classList.toggle(
        'hidden',
        currentUser?.role !== 'OWNER'
    );
}

function showApp() {
    $('#login').classList.add('hidden');
    $('#app').classList.remove('hidden');
    $('#logout').classList.remove('hidden');
}


$('#logout').onclick = async () => {
    await api('/api/auth/logout', {
        method: 'POST'
    }).catch(() => {
    });

    location.reload();
};


document.querySelectorAll('nav button').forEach(b => {
    b.onclick = () => showTab(b.dataset.tab);
});


function showTab(id) {
    document.querySelectorAll('.panel').forEach(x => {
        x.classList.add('hidden');
    });

    $('#' + id).classList.remove('hidden');
}


async function refresh() {
    members = await api('/api/admin/members');

    const sessions = await api('/api/admin/sessions');

    if (currentUser?.role === 'OWNER') {
        maintainers = await api('/api/owner/admins');
        renderMaintainers();
    }

    renderMembers();
    renderSessions(sessions);

    $('#summary').innerHTML = `
        <div class="metric">
            <b>${sessions.filter(s => s.status === 'OPEN').length}</b>
            <br>
            Open sessions
        </div>

        <div class="metric">
            <b>${members.filter(m => m.active).length}</b>
            <br>
            Active members
        </div>

        <div class="metric">
            <b>${sessions.filter(s => s.status === 'COMPLETED').length}</b>
            <br>
            Completed
        </div>
    `;
}


function renderMembers() {
    $('#memberList').innerHTML = members
        .map(m => `
            <div class="list-item">
                <b>${esc(m.displayName)}</b>
                · ${m.active ? 'Active' : 'Inactive'}
                · QR ${m.hasQr ? 'saved' : 'not set'}

                <button
                    class="ghost"
                    onclick="editMember(${m.id})">
                    Edit
                </button>

                <button
                    class="ghost"
                    onclick="uploadQr(${m.id})">
                    QR
                </button>
            </div>
        `)
        .join('');
}


function openMemberModal(id = null) {
    const member = id
        ? members.find(m => m.id === id)
        : null;

    $('#memberModalTitle').textContent =
        member ? 'Edit member' : 'Add member';

    $('#memberId').value =
        member?.id || '';

    $('#memberName').value =
        member?.name || '';

    $('#memberNickname').value =
        member?.nickname || '';

    $('#memberPaymentNote').value =
        member?.paymentNote || '';

    $('#memberActive').checked =
        member ? member.active : true;

    $('#memberModal').classList.remove('hidden');

    $('#memberName').focus();
}


function closeMemberModal() {
    $('#memberModal').classList.add('hidden');
}


window.editMember = id => {
    openMemberModal(id);
};


$('#addMember').onclick = () => {
    openMemberModal();
};


$('#closeMemberModal').onclick = closeMemberModal;
$('#cancelMemberModal').onclick = closeMemberModal;


$('#memberForm').onsubmit = async e => {
    e.preventDefault();

    const id = $('#memberId').value;

    const request = {
        name: $('#memberName').value.trim(),
        nickname: $('#memberNickname').value.trim(),
        paymentNote: $('#memberPaymentNote').value.trim(),
        active: $('#memberActive').checked
    };

    try {
        await api(
            id
                ? `/api/admin/members/${id}`
                : '/api/admin/members',
            {
                method: id ? 'PUT' : 'POST',
                body: JSON.stringify(request)
            }
        );

        closeMemberModal();
        await refresh();

    } catch (e) {
        alert(e.message);
    }
};

$('#memberModal').onclick = e => {
    if (e.target === $('#memberModal')) {
        closeMemberModal();
    }
};


window.uploadQr = id => {
    const inp = document.createElement('input');

    inp.type = 'file';
    inp.accept = 'image/*';

    inp.onchange = async () => {
        if (!inp.files[0]) {
            return;
        }

        const f = new FormData();
        f.append('file', inp.files[0]);

        await api(
            `/api/admin/members/${id}/qr`,
            {
                method: 'POST',
                body: f
            }
        );

        await refresh();
    };

    inp.click();
};


function renderSessions(ss) {
    $('#sessionList').innerHTML = ss
        .map(s => `
            <div class="list-item">
                <b>${esc(s.title)}</b>
                · ${formatDate(s.sessionDate)}
                ·
                <span class="status-badge status-${s.status.toLowerCase()}">
                    ${s.status}
                </span>
                · RM${Number(s.totalCost).toFixed(2)}

                <button
                    class="ghost"
                    onclick="editSession(${s.id})">
                    Open
                </button>

                ${s.status !== 'DRAFT'
                ? `
            <span class="session-actions">
            <a
                class="button ghost"
                target="_blank"
                href="/session.html?token=${encodeURIComponent(s.publicToken)}">
                View public page
            </a>

            <button
                class="ghost"
                onclick="copyPublicLink('${encodeURIComponent(s.publicToken)}')">
                Copy link
            </button>
        </span>
        `
                : ''
            }
            </div>
        `)
        .join('');
}

window.copyPublicLink = async token => {
    const url =
        `${window.location.origin}/session.html?token=${token}`;

    try {
        await navigator.clipboard.writeText(url);
        alert('Public link copied.');

    } catch {
        alert('Unable to copy link.');
    }
};


$('#newSession').onclick = () => {
    openEditor(null);
};


window.editSession = async id => {
    openEditor(
        await api(`/api/admin/sessions/${id}`)
    );
};


function openEditor(s) {
    showTab('editor');

    $('#sessionId').value = s?.id || '';
    $('#title').value = s?.title || '';
    $('#sessionDate').value =
        s?.sessionDate ||
        new Date().toISOString().slice(0, 10);

    $('#notes').value = s?.notes || '';

    const selected = new Set(
        (s?.participants || []).map(p => p.memberId)
    );

    $('#participantChecks').innerHTML = members
        .filter(m => m.active || selected.has(m.id))
        .map(m => `
            <label>
                <input
                    type="checkbox"
                    value="${m.id}"
                    ${selected.has(m.id) ? 'checked' : ''}>
                ${esc(m.displayName)}
            </label>
        `)
        .join('');

    $('#expenseRows').innerHTML = '';

    (s?.expenses || []).forEach(addExpenseRow);

    if (!s) {
        addExpenseRow();
    }

    $('#preview').textContent = '';

    renderAdminTransfers(
        s?.transfers || []
    );
}


function addExpenseRow(e = {}) {
    const d = document.createElement('div');

    d.className = 'expense-row';

    d.innerHTML = `
        <select class="etype">
            <option>COURT</option>
            <option>SHUTTLE</option>
            <option>OTHER</option>
        </select>

        <input
            class="edesc"
            placeholder="Description"
            value="${esc(e.description || '')}">

        <input
            class="eamount"
            type="number"
            step="0.01"
            min="0"
            placeholder="RM"
            value="${e.amount ?? ''}">

        <select class="epayer">
            ${members
            .map(m => `
                    <option value="${m.id}">
                        ${esc(m.displayName)}
                    </option>
                `)
            .join('')}
        </select>

        <button
            type="button"
            class="ghost">
            Remove
        </button>
    `;

    d.querySelector('.etype').value =
        e.type || 'COURT';

    d.querySelector('.epayer').value =
        e.paidByMemberId ||
        members[0]?.id ||
        '';

    d.querySelector('button').onclick = () => {
        d.remove();
    };

    $('#expenseRows').appendChild(d);
}


$('#addExpense').onclick = () => {
    addExpenseRow();
};


function payload(recalc = false) {
    return {
        title: $('#title').value,

        sessionDate:
            $('#sessionDate').value,

        notes:
            $('#notes').value,

        participantIds: [
            ...document.querySelectorAll(
                '#participantChecks input:checked'
            )
        ].map(x => Number(x.value)),

        expenses: [
            ...document.querySelectorAll('.expense-row')
        ]
            .filter(r =>
                r.querySelector('.eamount').value !== ''
            )
            .map(r => ({
                type:
                    r.querySelector('.etype').value,

                description:
                    r.querySelector('.edesc').value,

                amount:
                    Number(
                        r.querySelector('.eamount').value
                    ).toFixed(2),

                paidByMemberId:
                    Number(
                        r.querySelector('.epayer').value
                    )
            })),

        recalculateSettlement: recalc
    };
}


async function saveSession() {
    const id = $('#sessionId').value;

    try {
        const s = await api(
            id
                ? `/api/admin/sessions/${id}`
                : '/api/admin/sessions',
            {
                method: id ? 'PUT' : 'POST',
                body: JSON.stringify(
                    payload(false)
                )
            }
        );

        $('#sessionId').value = s.id;

        await refresh();

        return s;

    } catch (e) {
        if (
            e.status === 409 &&
            confirm(
                e.message +
                '\n\nRecalculate and reset affected payments?'
            )
        ) {
            const s = await api(
                `/api/admin/sessions/${id}`,
                {
                    method: 'PUT',
                    body: JSON.stringify(
                        payload(true)
                    )
                }
            );

            await refresh();

            return s;
        }

        throw e;
    }
}


$('#sessionForm').onsubmit = async e => {
    e.preventDefault();

    try {
        await saveSession();
        alert('Saved');

    } catch (err) {
        alert(err.message);
    }
};


$('#previewBtn').onclick = async () => {
    try {
        let id = $('#sessionId').value;

        if (!id) {
            await saveSession();
            id = $('#sessionId').value;
        } else {
            await saveSession();
        }

        const p = await api(
            `/api/admin/sessions/${id}/calculate`,
            {
                method: 'POST'
            }
        );

        $('#preview').textContent =
            formatPreview(p);

    } catch (e) {
        alert(e.message);
    }
};


$('#openBtn').onclick = async () => {
    try {
        await saveSession();

        const s = await api(
            `/api/admin/sessions/${$('#sessionId').value}/open`,
            {
                method: 'POST'
            }
        );

        alert(
            `Session opened. Public token: ${s.publicToken}`
        );

        await refresh();

    } catch (e) {
        alert(e.message);
    }
};


$('#completeBtn').onclick = async () => {
    try {
        await api(
            `/api/admin/sessions/${$('#sessionId').value}/complete`,
            {
                method: 'POST'
            }
        );

        alert('Session completed');

        await refresh();

    } catch (e) {
        alert(e.message);
    }
};


$('#reopenBtn').onclick = async () => {
    try {
        await api(
            `/api/admin/sessions/${$('#sessionId').value}/reopen`,
            {
                method: 'POST'
            }
        );

        alert('Session reopened');

        await refresh();

        openEditor(
            await api(
                `/api/admin/sessions/${$('#sessionId').value}`
            )
        );

    } catch (e) {
        alert(e.message);
    }
};


$('#closeEditor').onclick = () => {
    showTab('sessions');
    refresh();
};


function renderAdminTransfers(ts) {
    $('#paymentAdmin').innerHTML = ts.length
        ? `
            <h3>Payment status</h3>
            ${ts.map(t => `
                    <div class="list-item">
                        <b>
                            ${esc(t.payerName)}
                            →
                            ${esc(t.payeeName)}
                        </b>

                        RM${Number(t.amount).toFixed(2)}
                        · ${t.status}

                        ${t.status === 'PAID'
                ? `
                                    <button
                                        class="ghost"
                                        onclick="resetTransfer(${t.id})">
                                        Mark unpaid
                                    </button>
                                `
                : ''
            }
                    </div>
                `).join('')
        }
        `
        : '';
}


window.resetTransfer = async id => {
    try {
        await api(
            `/api/admin/transfers/${id}/mark-unpaid`,
            {
                method: 'POST'
            }
        );

        const sid =
            $('#sessionId').value;

        if (sid) {
            openEditor(
                await api(
                    `/api/admin/sessions/${sid}`
                )
            );
        }

    } catch (e) {
        alert(e.message);
    }
};


function formatPreview(p) {
    return `
Total: RM${Number(p.totalCost).toFixed(2)}

Balances:
${p.balances
            .map(b =>
                `${memberName(b.memberId)}  ${Number(b.netBalance) >= 0
                    ? '+'
                    : ''
                }RM${Number(b.netBalance).toFixed(2)}`
            )
            .join('\n')}

Transfers:
${p.transfers.length
            ? p.transfers
                .map(t =>
                    `${memberName(t.payerMemberId)} -> ${memberName(t.payeeMemberId)} RM${Number(t.amount).toFixed(2)}`
                )
                .join('\n')
            : 'No transfers required'
        }
    `.trim();
}


function memberName(id) {
    return (
        members.find(m => m.id === id)?.displayName ||
        `Member ${id}`
    );
}


function esc(s) {
    return String(s ?? '')
        .replace(
            /[&<>'"]/g,
            c => ({
                '&': '&amp;',
                '<': '&lt;',
                '>': '&gt',
                "'": '&#39;',
                '"': '&quot;'
            }[c])
        );
}

function renderMaintainers() {
    $('#maintainerList').innerHTML = maintainers.length
        ? maintainers.map(m => `
            <div class="list-item">
                <b>${esc(m.username)}</b>
                · ${m.active ? 'Active' : 'Inactive'}

                <button
                    class="ghost"
                    onclick="toggleMaintainer(${m.id}, ${!m.active})">
                    ${m.active ? 'Deactivate' : 'Activate'}
                </button>

                <button
                    class="ghost"
                    onclick="resetMaintainerPassword(${m.id})">
                    Reset password
                </button>
            </div>
        `).join('')
        : '<p>No maintainer accounts yet.</p>';
}


$('#addMaintainer').onclick = async () => {
    const username = prompt('Maintainer username');

    if (!username) {
        return;
    }

    const password = prompt('Temporary password');

    if (!password) {
        return;
    }

    try {
        await api('/api/owner/admins', {
            method: 'POST',
            body: JSON.stringify({
                username,
                password
            })
        });

        await refresh();

        alert('Maintainer account created.');

    } catch (e) {
        alert(e.message);
    }
};


window.toggleMaintainer = async (id, active) => {
    try {
        await api(
            `/api/owner/admins/${id}/active`,
            {
                method: 'PUT',
                body: JSON.stringify({
                    active
                })
            }
        );

        await refresh();

    } catch (e) {
        alert(e.message);
    }
};


window.resetMaintainerPassword = async id => {
    const maintainer = maintainers.find(m => m.id === id);

    const newPassword = prompt(
        `New password for ${maintainer?.username || 'maintainer'}`
    );

    if (!newPassword) {
        return;
    }

    try {
        await api(
            `/api/owner/admins/${id}/password`,
            {
                method: 'PUT',
                body: JSON.stringify({
                    newPassword
                })
            }
        );

        alert('Password reset successfully.');

    } catch (e) {
        alert(e.message);
    }
};

function formatDate(dateString) {
    if (!dateString) {
        return '';
    }

    const [year, month, day] = dateString.split('-');

    return `${day}-${month}-${year}`;
}


boot();