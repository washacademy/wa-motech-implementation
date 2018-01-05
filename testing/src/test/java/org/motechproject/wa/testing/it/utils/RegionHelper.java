package org.motechproject.wa.testing.it.utils;

import org.motechproject.wa.region.domain.*;
import org.motechproject.wa.region.domain.Block;
import org.motechproject.wa.region.repository.CircleDataService;
import org.motechproject.wa.region.repository.DistrictDataService;
import org.motechproject.wa.region.repository.LanguageDataService;
import org.motechproject.wa.region.repository.StateDataService;
import org.motechproject.wa.region.service.DistrictService;
import org.motechproject.wa.region.service.LanguageService;

public class RegionHelper {
    private LanguageDataService languageDataService;
    private LanguageService languageService;
    private CircleDataService circleDataService;
    private DistrictDataService districtDataService;
    private DistrictService districtService;
    private StateDataService stateDataService;

    public RegionHelper(LanguageDataService languageDataService,
                        LanguageService languageService,
                        CircleDataService circleDataService,
                        StateDataService stateDataService,
                        DistrictDataService districtDataService,
                        DistrictService districtService) {

        this.languageDataService = languageDataService;
        this.languageService = languageService;
        this.circleDataService = circleDataService;
        this.stateDataService = stateDataService;
        this.districtDataService = districtDataService;
        this.districtService = districtService;
    }

    public Circle delhiCircle() {
        Circle c = circleDataService.findByName("DE");

        if (c == null) {
            c = new Circle("DE");

            c.setDefaultLanguage(hindiLanguage());
            circleDataService.create(c);

            // Create the dehli district which is linked to this circle.  Also creates state
            newDelhiDistrict();
        }

        return c;
    }

    public Circle karnatakaCircle() {
        Circle c = circleDataService.findByName("KA");

        if (c == null) {
            c = new Circle("KA");
            c.setDefaultLanguage(kannadaLanguage());
            circleDataService.create(c);

            // Create a district which also creates the link and the state
            bangaloreDistrict();
        }

        return c;
    }

    public State delhiState() {
        State s = stateDataService.findByCode(1l);

        if (s == null) {
            s = new State();
            s.setName("National Capital Territory of Delhi");
            s.setCode(1L);
            stateDataService.create(s);
        }

        return s;
    }


    public State karnatakaState() {
        State s = stateDataService.findByCode(2l);

        if (s == null) {
            s = new State();
            s.setName("Karnataka");
            s.setCode(2L);
            stateDataService.create(s);
        }

        return s;
    }


    public District newDelhiDistrict() {
        delhiState();
        delhiCircle();

        District d = districtService.findByStateAndCode(delhiState(), 1L);

        if (d == null) {
            d = new District();
            d.setName("New Delhi");
            d.setRegionalName("New Delhi");
            d.setCode(1L);
            d.setState(delhiState());
            d.setLanguage(hindiLanguage());
            d.setCircle(delhiCircle());
            districtDataService.create(d);

            stateDataService.evictAllCache();
        }

        return d;
    }


    public District southDelhiDistrict() {
        District d = districtService.findByStateAndCode(delhiState(), 5L);

        if (d == null) {
            d = new District();
            d.setName("South Delhi");
            d.setRegionalName("South Delhi");
            d.setCode(5L);
            d.setState(delhiState());
            d.setCircle(delhiCircle());
            d.setLanguage(punjabiLanguage());
            districtDataService.create(d);

            stateDataService.evictAllCache();
        }

        return d;
    }


    public District bangaloreDistrict() {
        karnatakaState();
        karnatakaCircle();

        District d = districtService.findByStateAndCode(karnatakaState(), 4L);

        if (d == null) {
            d = new District();
            d.setName("Bengaluru");
            d.setRegionalName("Bengaluru");
            d.setCode(4L);
            d.setState(karnatakaState());
            d.setCircle(karnatakaCircle());
            d.setLanguage(tamilLanguage());
            districtDataService.create(d);

            stateDataService.evictAllCache();
        }

        return d;
    }


    public District mysuruDistrict() {
        District d = districtService.findByStateAndCode(karnatakaState(), 2L);

        if (d == null) {
            d = new District();
            d.setName("Mysuru");
            d.setRegionalName("Mysuru");
            d.setCode(2L);
            d.setState(karnatakaState());
            d.setCircle(karnatakaCircle());
            d.setLanguage(kannadaLanguage());
            districtDataService.create(d);

            stateDataService.evictAllCache();
        }

        return d;
    }


    public Language tamilLanguage() {
        Language l = languageService.getForName("Tamil");

        if (l == null) {
            l = languageDataService.create(new Language("ta", "Tamil"));
        }

        return l;
    }


    public Language kannadaLanguage() {
        Language l = languageService.getForName("Kannada");

        if (l == null) {
            l = languageDataService.create(new Language("kn", "Kannada"));
        }

        return l;
    }


    public Language punjabiLanguage() {
        Language l = languageService.getForName("Punjabi");

        if (l == null) {
            l = languageDataService.create(new Language("pa", "Punjabi"));
        }

        return l;
    }


    public Language hindiLanguage() {
        Language l = languageService.getForName("Hindi");

        if (l == null) {
            l = languageDataService.create(new Language("hi", "Hindi"));
        }

        return l;
    }

    public String airtelOperator()
    {
        return "A";
    }

    public static State createState(Long code, String name) {
        State state = new State();
        state.setCode(code);
        state.setName(name);
        return state;
    }

    public static District createDistrict(State state, Long code, String name) {
        return createDistrict(state, code, name, null, null);
    }

    public static District createDistrict(State state, Long code, String name, Language language) {
        return createDistrict(state, code, name, language, null);
    }

    public static District createDistrict(State state, Long code, String name, Circle circle) {
        return createDistrict(state, code, name, null, circle);
    }

    public static District createDistrict(State state, Long code, String name, Language language, Circle circle) {
        District district = new District();
        district.setState(state);
        district.setCode(code);
        district.setName(name);
        district.setRegionalName(regionalName(name));
        district.setLanguage(language);

        if (circle != null) {
            district.setCircle(circle);
            circle.getDistricts().add(district);
        }

        return district;
    }

    public static Language createLanguage(String code, String name) {
        return new Language(code, name);
    }

    public static Block createTaluka(District district, Long code, String name, int identity) {
        Block block = new Block();
        block.setDistrict(district);
        block.setCode(code);
        block.setName(name);
        block.setRegionalName(regionalName(name));
        block.setIdentity(identity);
        return block;
    }

//    public static HealthBlock createHealthBlock(Block block, Long code, String name, String hq) {
//        HealthBlock healthBlock = new HealthBlock();
//        healthBlock.setBlock(block);
//        healthBlock.setCode(code);
//        healthBlock.setName(name);
//        healthBlock.setRegionalName(regionalName(name));
//        healthBlock.setHq(hq);
//        return healthBlock;
//    }

    public static Panchayat createVillage(Block block, long svid, long vcode, String name) {
        Panchayat panchayat = new Panchayat();
        panchayat.setBlock(block);
        panchayat.setSvid(svid);
        panchayat.setVcode(vcode);
        panchayat.setName(name);
        panchayat.setRegionalName(regionalName(name));
        return panchayat;
    }

//    public static HealthFacility createHealthFacility(HealthBlock healthBlock, Long code, String name, HealthFacilityType type) {
//        HealthFacility healthFacility = new HealthFacility();
//        healthFacility.setHealthBlock(healthBlock);
//        healthFacility.setCode(code);
//        healthFacility.setName(name);
//        healthFacility.setRegionalName(regionalName(name));
//        healthFacility.setHealthFacilityType(type);
//        return healthFacility;
//    }
//
//    public static HealthFacilityType createHealthFacilityType(String name, Long code) {
//        HealthFacilityType healthFacilityType = new HealthFacilityType();
//        healthFacilityType.setName(name);
//        healthFacilityType.setCode(code);
//        return healthFacilityType;
//    }
//
//    public static HealthSubFacility createHealthSubFacility(String name, Long code, HealthFacility healthFacility) {
//        HealthSubFacility healthSubFacility = new HealthSubFacility();
//        healthSubFacility.setName(name);
//        healthSubFacility.setCode(code);
//        healthSubFacility.setRegionalName(name + " regional name");
//        healthSubFacility.setHealthFacility(healthFacility);
//        return healthSubFacility;
//    }

    public static Language createLanguage(String code, String name, Circle circle, boolean defaultForCircle, District... districts) {
        Language language = new Language();
        language.setCode(code);
        language.setName(name);
        for (District district : districts) {
            district.setLanguage(language);
        }
        if (defaultForCircle) {
            circle.setDefaultLanguage(language);
        }

        return language;
    }

    public static Circle createCircle(String name) {
        return new Circle(name);
    }

    public static String regionalName(String name) {
        return String.format("regional name of %s", name);
    }

}
