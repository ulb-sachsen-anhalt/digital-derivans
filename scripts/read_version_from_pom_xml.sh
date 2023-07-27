read_version_from_pom_xml() {

    if [ $# -ne 1 ]; then
        echo "no pom_xml_dir path provided"
        return 1 # (exitcode)
    fi
    local pom_xml_dir="$(realpath "$1")"
    local pom_xml_file="$pom_xml_dir/pom.xml"

    # Check if package.json exists
    if [ -f "$pom_xml_file" ]; then
        # Extract the version field using pattern matching
        local version=$(grep -m 1 -oP '(?<=<version>)\d\.\d\.\d(?=</version>)' "$pom_xml_file")
        echo "$version"
        return 0
    else
        echo "Error: pom.xml not found in the directory $pom_xml_dir"
        return 1
    fi

}

if [ "$0" = "$BASH_SOURCE" ]; then
    read_version_from_pom_xml "$@"
fi
