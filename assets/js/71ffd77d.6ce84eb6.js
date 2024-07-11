"use strict";(self.webpackChunkreftree=self.webpackChunkreftree||[]).push([[533],{3691:(e,t,r)=>{r.r(t),r.d(t,{assets:()=>u,contentTitle:()=>o,default:()=>p,frontMatter:()=>l,metadata:()=>c,toc:()=>d});var n=r(4848),a=r(8453),i=r(1470),s=r(9365);const l={sidebar_position:2},o="Getting started",c={id:"GettingStarted",title:"Getting started",description:"To use this library you will need to have GraphViz installed (and have dot on your PATH).",source:"@site/../site-gen/target/mdoc/GettingStarted.md",sourceDirName:".",slug:"/GettingStarted",permalink:"/reftree/docs/GettingStarted",draft:!1,unlisted:!1,editUrl:"https://github.com/stanch/reftree/tree/main/docs/../site-gen/target/mdoc/GettingStarted.md",tags:[],version:"current",sidebarPosition:2,frontMatter:{sidebar_position:2},sidebar:"mainSidebar",previous:{title:"Overview",permalink:"/reftree/docs/"},next:{title:"User guide",permalink:"/reftree/docs/Guide"}},u={},d=[{value:"Interactive usage",id:"interactive-usage",level:2},{value:"Including in your project",id:"including-in-your-project",level:2},{value:"Minimal example",id:"minimal-example",level:2}];function h(e){const t={a:"a",code:"code",em:"em",h1:"h1",h2:"h2",img:"img",p:"p",pre:"pre",...(0,a.R)(),...e.components};return(0,n.jsxs)(n.Fragment,{children:[(0,n.jsx)(t.h1,{id:"getting-started",children:"Getting started"}),"\n",(0,n.jsxs)(t.p,{children:["To use this library you will need to have ",(0,n.jsx)(t.a,{href:"http://www.graphviz.org/",children:"GraphViz"})," installed (and have ",(0,n.jsx)(t.code,{children:"dot"})," on your ",(0,n.jsx)(t.code,{children:"PATH"}),").\nI also recommend to install the ",(0,n.jsx)(t.a,{href:"https://github.com/adobe-fonts/source-code-pro",children:"Source Code Pro"})," fonts (regular and ",(0,n.jsx)(t.em,{children:"italic"}),"),\nas I find they look the best among the free options and therefore are used by default."]}),"\n",(0,n.jsxs)(t.p,{children:["For viewing PNG and animated GIF on Linux I recommend ",(0,n.jsx)(t.code,{children:"eog"})," and ",(0,n.jsx)(t.code,{children:"gifview"})," respectively."]}),"\n",(0,n.jsx)(t.h2,{id:"interactive-usage",children:"Interactive usage"}),"\n",(0,n.jsx)(t.p,{children:"To jump into an interactive session:"}),"\n",(0,n.jsx)(t.pre,{children:(0,n.jsx)(t.code,{children:"$ git clone https://github.com/stanch/reftree\n$ cd reftree\n$ sbt demo\n@ render(List(1, 2, 3))\n// display diagram.png with your favorite image viewer\n"})}),"\n",(0,n.jsx)(t.h2,{id:"including-in-your-project",children:"Including in your project"}),"\n",(0,n.jsxs)(t.p,{children:[(0,n.jsx)(t.code,{children:"reftree"})," is available for Scala 2.12 and 2.13. You can depend on the library by adding these lines to your ",(0,n.jsx)(t.code,{children:"build.sbt"}),":"]}),"\n","\n",(0,n.jsxs)(i.A,{groupId:"platform",children:[(0,n.jsx)(s.A,{value:"jvm",label:"JVM",default:!0,children:(0,n.jsx)(t.pre,{children:(0,n.jsx)(t.code,{className:"language-scala",children:'libraryDependencies += "io.github.stanch" %% "reftree" % "1.5.0"\n'})})}),(0,n.jsx)(s.A,{value:"js",label:"Scala.js 1.16+",children:(0,n.jsx)(t.pre,{children:(0,n.jsx)(t.code,{className:"language-scala",children:'libraryDependencies += "io.github.stanch" %%% "reftree" % "1.5.0"\n'})})})]}),"\n",(0,n.jsx)(t.h2,{id:"minimal-example",children:"Minimal example"}),"\n",(0,n.jsx)(t.pre,{children:(0,n.jsx)(t.code,{className:"language-scala",children:"import reftree.render.{Renderer, RenderingOptions}\nimport reftree.diagram.Diagram\nimport java.nio.file.Paths\n"})}),"\n",(0,n.jsx)(t.pre,{children:(0,n.jsx)(t.code,{className:"language-scala",children:'val ImagePath = "images"\n'})}),"\n",(0,n.jsx)(t.pre,{children:(0,n.jsx)(t.code,{className:"language-scala",children:'val renderer = Renderer(\n  renderingOptions = RenderingOptions(density = 100),\n  directory = Paths.get(ImagePath, "overview")\n)\nimport renderer._\n\ncase class Person(firstName: String, age: Int)\n\nDiagram.sourceCodeCaption(Person("Bob", 42)).render("example")\n'})}),"\n",(0,n.jsxs)(t.p,{children:["This generates ",(0,n.jsx)(t.code,{children:"images/overview/example.png"})," with the following image:"]}),"\n",(0,n.jsx)(t.p,{children:(0,n.jsx)(t.img,{alt:"bob",src:r(8710).A+"",width:"367",height:"363"})}),"\n",(0,n.jsxs)(t.p,{children:["For more details, please refer to the ",(0,n.jsx)(t.a,{href:"/reftree/docs/Guide",children:"guide"}),"."]})]})}function p(e={}){const{wrapper:t}={...(0,a.R)(),...e.components};return t?(0,n.jsx)(t,{...e,children:(0,n.jsx)(h,{...e})}):h(e)}},9365:(e,t,r)=>{r.d(t,{A:()=>s});r(6540);var n=r(4164);const a={tabItem:"tabItem_Ymn6"};var i=r(4848);function s(e){let{children:t,hidden:r,className:s}=e;return(0,i.jsx)("div",{role:"tabpanel",className:(0,n.A)(a.tabItem,s),hidden:r,children:t})}},1470:(e,t,r)=>{r.d(t,{A:()=>w});var n=r(6540),a=r(4164),i=r(3104),s=r(6347),l=r(205),o=r(7485),c=r(1682),u=r(679);function d(e){return n.Children.toArray(e).filter((e=>"\n"!==e)).map((e=>{if(!e||(0,n.isValidElement)(e)&&function(e){const{props:t}=e;return!!t&&"object"==typeof t&&"value"in t}(e))return e;throw new Error(`Docusaurus error: Bad <Tabs> child <${"string"==typeof e.type?e.type:e.type.name}>: all children of the <Tabs> component should be <TabItem>, and every <TabItem> should have a unique "value" prop.`)}))?.filter(Boolean)??[]}function h(e){const{values:t,children:r}=e;return(0,n.useMemo)((()=>{const e=t??function(e){return d(e).map((e=>{let{props:{value:t,label:r,attributes:n,default:a}}=e;return{value:t,label:r,attributes:n,default:a}}))}(r);return function(e){const t=(0,c.X)(e,((e,t)=>e.value===t.value));if(t.length>0)throw new Error(`Docusaurus error: Duplicate values "${t.map((e=>e.value)).join(", ")}" found in <Tabs>. Every value needs to be unique.`)}(e),e}),[t,r])}function p(e){let{value:t,tabValues:r}=e;return r.some((e=>e.value===t))}function m(e){let{queryString:t=!1,groupId:r}=e;const a=(0,s.W6)(),i=function(e){let{queryString:t=!1,groupId:r}=e;if("string"==typeof t)return t;if(!1===t)return null;if(!0===t&&!r)throw new Error('Docusaurus error: The <Tabs> component groupId prop is required if queryString=true, because this value is used as the search param name. You can also provide an explicit value such as queryString="my-search-param".');return r??null}({queryString:t,groupId:r});return[(0,o.aZ)(i),(0,n.useCallback)((e=>{if(!i)return;const t=new URLSearchParams(a.location.search);t.set(i,e),a.replace({...a.location,search:t.toString()})}),[i,a])]}function g(e){const{defaultValue:t,queryString:r=!1,groupId:a}=e,i=h(e),[s,o]=(0,n.useState)((()=>function(e){let{defaultValue:t,tabValues:r}=e;if(0===r.length)throw new Error("Docusaurus error: the <Tabs> component requires at least one <TabItem> children component");if(t){if(!p({value:t,tabValues:r}))throw new Error(`Docusaurus error: The <Tabs> has a defaultValue "${t}" but none of its children has the corresponding value. Available values are: ${r.map((e=>e.value)).join(", ")}. If you intend to show no default tab, use defaultValue={null} instead.`);return t}const n=r.find((e=>e.default))??r[0];if(!n)throw new Error("Unexpected error: 0 tabValues");return n.value}({defaultValue:t,tabValues:i}))),[c,d]=m({queryString:r,groupId:a}),[g,f]=function(e){let{groupId:t}=e;const r=function(e){return e?`docusaurus.tab.${e}`:null}(t),[a,i]=(0,u.Dv)(r);return[a,(0,n.useCallback)((e=>{r&&i.set(e)}),[r,i])]}({groupId:a}),b=(()=>{const e=c??g;return p({value:e,tabValues:i})?e:null})();(0,l.A)((()=>{b&&o(b)}),[b]);return{selectedValue:s,selectValue:(0,n.useCallback)((e=>{if(!p({value:e,tabValues:i}))throw new Error(`Can't select invalid tab value=${e}`);o(e),d(e),f(e)}),[d,f,i]),tabValues:i}}var f=r(2303);const b={tabList:"tabList__CuJ",tabItem:"tabItem_LNqP"};var v=r(4848);function x(e){let{className:t,block:r,selectedValue:n,selectValue:s,tabValues:l}=e;const o=[],{blockElementScrollPositionUntilNextRender:c}=(0,i.a_)(),u=e=>{const t=e.currentTarget,r=o.indexOf(t),a=l[r].value;a!==n&&(c(t),s(a))},d=e=>{let t=null;switch(e.key){case"Enter":u(e);break;case"ArrowRight":{const r=o.indexOf(e.currentTarget)+1;t=o[r]??o[0];break}case"ArrowLeft":{const r=o.indexOf(e.currentTarget)-1;t=o[r]??o[o.length-1];break}}t?.focus()};return(0,v.jsx)("ul",{role:"tablist","aria-orientation":"horizontal",className:(0,a.A)("tabs",{"tabs--block":r},t),children:l.map((e=>{let{value:t,label:r,attributes:i}=e;return(0,v.jsx)("li",{role:"tab",tabIndex:n===t?0:-1,"aria-selected":n===t,ref:e=>o.push(e),onKeyDown:d,onClick:u,...i,className:(0,a.A)("tabs__item",b.tabItem,i?.className,{"tabs__item--active":n===t}),children:r??t},t)}))})}function j(e){let{lazy:t,children:r,selectedValue:a}=e;const i=(Array.isArray(r)?r:[r]).filter(Boolean);if(t){const e=i.find((e=>e.props.value===a));return e?(0,n.cloneElement)(e,{className:"margin-top--md"}):null}return(0,v.jsx)("div",{className:"margin-top--md",children:i.map(((e,t)=>(0,n.cloneElement)(e,{key:t,hidden:e.props.value!==a})))})}function y(e){const t=g(e);return(0,v.jsxs)("div",{className:(0,a.A)("tabs-container",b.tabList),children:[(0,v.jsx)(x,{...t,...e}),(0,v.jsx)(j,{...t,...e})]})}function w(e){const t=(0,f.A)();return(0,v.jsx)(y,{...e,children:d(e.children)},String(t))}},8710:(e,t,r)=>{r.d(t,{A:()=>n});const n=r.p+"assets/images/example-279830c17e2bfcec051c7a3c6280e740.png"},8453:(e,t,r)=>{r.d(t,{R:()=>s,x:()=>l});var n=r(6540);const a={},i=n.createContext(a);function s(e){const t=n.useContext(i);return n.useMemo((function(){return"function"==typeof e?e(t):{...t,...e}}),[t,e])}function l(e){let t;return t=e.disableParentContext?"function"==typeof e.components?e.components(a):e.components||a:s(e.components),n.createElement(i.Provider,{value:t},e.children)}}}]);