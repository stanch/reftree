"use strict";(self.webpackChunkreftree=self.webpackChunkreftree||[]).push([[350],{5011:(e,t,n)=>{n.r(t),n.d(t,{assets:()=>$,contentTitle:()=>v,default:()=>P,frontMatter:()=>F,metadata:()=>T,toc:()=>A});var r=n(4848),s=n(8453),o=n(6540),i=n(4164),c=n(1754),l=n(8774),a=n(4586);const u=["zero","one","two","few","many","other"];function d(e){return u.filter((t=>e.includes(t)))}const m={locale:"en",pluralForms:d(["one","other"]),select:e=>1===e?"one":"other"};function f(){const{i18n:{currentLocale:e}}=(0,a.A)();return(0,o.useMemo)((()=>{try{return function(e){const t=new Intl.PluralRules(e);return{locale:e,pluralForms:d(t.resolvedOptions().pluralCategories),select:e=>t.select(e)}}(e)}catch(t){return console.error(`Failed to use Intl.PluralRules for locale "${e}".\nDocusaurus will fallback to the default (English) implementation.\nError: ${t.message}\n`),m}}),[e])}function p(){const e=f();return{selectMessage:(t,n)=>function(e,t,n){const r=e.split("|");if(1===r.length)return r[0];r.length>n.pluralForms.length&&console.error(`For locale=${n.locale}, a maximum of ${n.pluralForms.length} plural forms are expected (${n.pluralForms.join(",")}), but the message contains ${r.length}: ${e}`);const s=n.select(t),o=n.pluralForms.indexOf(s);return r[Math.min(o,r.length-1)]}(n,t,e)}}var h=n(6654),g=n(1312),x=n(1107);const k={cardContainer:"cardContainer_fWXF",cardTitle:"cardTitle_rnsV",cardDescription:"cardDescription_PWke"};function j(e){let{href:t,children:n}=e;return(0,r.jsx)(l.A,{href:t,className:(0,i.A)("card padding--lg",k.cardContainer),children:n})}function b(e){let{href:t,icon:n,title:s,description:o}=e;return(0,r.jsxs)(j,{href:t,children:[(0,r.jsxs)(x.A,{as:"h2",className:(0,i.A)("text--truncate",k.cardTitle),title:s,children:[n," ",s]}),o&&(0,r.jsx)("p",{className:(0,i.A)("text--truncate",k.cardDescription),title:o,children:o})]})}function y(e){let{item:t}=e;const n=(0,c.Nr)(t),s=function(){const{selectMessage:e}=p();return t=>e(t,(0,g.T)({message:"1 item|{count} items",id:"theme.docs.DocCard.categoryDescription.plurals",description:"The default description for a category card in the generated index about how many items this category includes"},{count:t}))}();return n?(0,r.jsx)(b,{href:n,icon:"\ud83d\uddc3\ufe0f",title:t.label,description:t.description??s(t.items.length)}):null}function w(e){let{item:t}=e;const n=(0,h.A)(t.href)?"\ud83d\udcc4\ufe0f":"\ud83d\udd17",s=(0,c.cC)(t.docId??void 0);return(0,r.jsx)(b,{href:t.href,icon:n,title:t.label,description:t.description??s?.description})}function C(e){let{item:t}=e;switch(t.type){case"link":return(0,r.jsx)(w,{item:t});case"category":return(0,r.jsx)(y,{item:t});default:throw new Error(`unknown item type ${JSON.stringify(t)}`)}}function N(e){let{className:t}=e;const n=(0,c.$S)();return(0,r.jsx)(D,{items:n.items,className:t})}function D(e){const{items:t,className:n}=e;if(!t)return(0,r.jsx)(N,{...e});const s=(0,c.d1)(t);return(0,r.jsx)("section",{className:(0,i.A)("row",n),children:s.map(((e,t)=>(0,r.jsx)("article",{className:"col col--6 margin-bottom--lg",children:(0,r.jsx)(C,{item:e})},t)))})}const F={sidebar_position:4},v="Talks / Demos",T={id:"talks/index",title:"Talks / Demos",description:"",source:"@site/../site-gen/target/mdoc/talks/index.md",sourceDirName:"talks",slug:"/talks/",permalink:"/reftree/docs/talks/",draft:!1,unlisted:!1,editUrl:"https://github.com/stanch/reftree/tree/main/docs/../site-gen/target/mdoc/talks/index.md",tags:[],version:"current",sidebarPosition:4,frontMatter:{sidebar_position:4},sidebar:"mainSidebar",previous:{title:"User guide",permalink:"/reftree/docs/Guide"},next:{title:"Unzipping Immutability",permalink:"/reftree/docs/talks/Immutability"}},$={},A=[];function M(e){const t={h1:"h1",...(0,s.R)(),...e.components};return(0,r.jsxs)(r.Fragment,{children:[(0,r.jsx)(t.h1,{id:"talks--demos",children:"Talks / Demos"}),"\n","\n",(0,r.jsx)(D,{})]})}function P(e={}){const{wrapper:t}={...(0,s.R)(),...e.components};return t?(0,r.jsx)(t,{...e,children:(0,r.jsx)(M,{...e})}):M(e)}},8453:(e,t,n)=>{n.d(t,{R:()=>i,x:()=>c});var r=n(6540);const s={},o=r.createContext(s);function i(e){const t=r.useContext(o);return r.useMemo((function(){return"function"==typeof e?e(t):{...t,...e}}),[t,e])}function c(e){let t;return t=e.disableParentContext?"function"==typeof e.components?e.components(s):e.components||s:i(e.components),r.createElement(o.Provider,{value:t},e.children)}}}]);