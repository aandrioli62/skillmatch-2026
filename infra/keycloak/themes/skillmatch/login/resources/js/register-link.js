// Adds a "Non hai un account? Registrati" link below the username/password
// form, so a visitor who mistakenly clicked "Accedi" instead of "Registrati"
// on the SkillMatch app's own landing page doesn't have to use the browser's
// back button. Only self-service registration lives in the app (Keycloak's
// own registrationAllowed is off), so the target URL is derived from the
// current request's redirect_uri (the app's own origin) rather than
// hardcoded, which keeps this working identically in local dev and in
// production.
(function () {
  document.addEventListener('DOMContentLoaded', function () {
    var form = document.getElementById('kc-form-login');
    if (!form || !form.parentNode) return;

    var params = new URLSearchParams(window.location.search);
    var redirectUri = params.get('redirect_uri');
    if (!redirectUri) return;

    var origin;
    try {
      origin = new URL(decodeURIComponent(redirectUri)).origin;
    } catch (e) {
      return;
    }

    var wrapper = document.createElement('div');
    wrapper.style.textAlign = 'center';
    wrapper.style.marginTop = '1.5rem';
    wrapper.innerHTML =
      '<span>Non hai un account?</span> ' +
      '<a href="' + origin + '/register" style="color: var(--pf-v5-global--link--Color, #4f46e5); font-weight: 600;">' +
      'Registrati</a>';

    form.parentNode.appendChild(wrapper);
  });
})();
