/**
 * @FileName : adAiRagManage.js
 * @Description :
 * @Author : 송혜진
 * @Date : 2026. 05. 11
 * @Modification_History
 * @
 * @ 수정일         수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.11    송혜진        최초 생성
 */

 document.addEventListener("DOMContentLoaded", function () {
     const tooltipTriggerList = [].slice.call(
         document.querySelectorAll('[data-bs-toggle="tooltip"]')
     );

     tooltipTriggerList.forEach(function (tooltipTriggerEl) {
         new bootstrap.Tooltip(tooltipTriggerEl);
     });
 });