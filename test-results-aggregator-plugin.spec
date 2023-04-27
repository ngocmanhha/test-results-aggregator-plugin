%global     plugin_name test-results-aggregator-plugin
%global     debug_package %{nil}
Name:       jenkins-in-house-plugins-%{plugin_name}
Version:    1.1.13
Release:    3%{?dist}
Summary:    A jenkins in-house plugins %{plugin_name}.hpi
Obsoletes:  jenkins-upstream-plugins-%{plugin_name} <= %{version}
Requires:   jenkins
Group:      Development/Libraries
License:    BSD
URL:        https://github.com/gooddata/%{plugin_name}
Source0:    %{name}.tar.gz

BuildRequires: java-1.8.0-openjdk-devel, maven >= 3.5.0, maven-openjdk8 >= 3.5.0
Requires: java-1.8.0-openjdk-devel, maven-openjdk8 >= 3.5.0

%description
Packaged jenkins-in-house-plugin-%{plugin_name} %{plugin_name}.hpi file

%prep
%setup -n %{name} -c 

%build
mvn versions:set -DnewVersion=%{version}
mvn versions:commit
mvn package --batch-mode -Dmaven.test.skip=true
# temporary skip test for now until we done refactor unit test for this rpm

%install
%{__mkdir_p} %{buildroot}%{_sharedstatedir}/juseppe
%{__cp} target/%{plugin_name}.hpi %{buildroot}%{_sharedstatedir}/juseppe/

%files
%defattr(-,root,root,-)
%dir %{_sharedstatedir}/juseppe
%{_sharedstatedir}/juseppe/%{plugin_name}.hpi

%changelog
* Thu Apr 27 2023 +0700 Manh Ha <manh.ha@gooddata.com> - 1.1.13-3
- Lock java version in rpm build
- Bump test-results-aggregator-plugin version

* Wed Dec 21 2022 +0700 Manh Ha <manh.ha@gooddata.com> - 1.1.13-2
- Correct cast property value object
- Bump test-results-aggregator-plugin version

* Wed Dec 21 2022 +0700 Manh Ha <manh.ha@gooddata.com> - 1.1.13-1
- Add success jobs in the summary report
- Fix missing aborted jobs in the summary report
- Fix typo of variable name
- Bump test-results-aggregator-plugin version

* Thu Dec 01 2022 +0700 Manh Ha <manh.ha@gooddata.com> - 1.1.12-1
- Reformat the code
- Add build number on the reports
- Bump test-results-aggregator-plugin version

* Wed Nov 09 2022 +0700 Manh Ha <manh.ha@gooddata.com> - 1.1.11-1
- CONFIG: SETI-7554 build rpm package
- Support collect test results based on build number for pipeline type
